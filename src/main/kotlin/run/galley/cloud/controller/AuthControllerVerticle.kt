package run.galley.cloud.controller

import generated.jooq.enums.MemberStatus
import generated.jooq.enums.VesselRole
import generated.jooq.tables.Crew.Companion.CREW
import generated.jooq.tables.Users.Companion.USERS
import generated.jooq.tables.pojos.Crew
import generated.jooq.tables.pojos.CrewCharterMember
import generated.jooq.tables.pojos.Sessions
import generated.jooq.tables.pojos.Users
import generated.jooq.tables.pojos.Vessels
import generated.jooq.tables.references.CREW_CHARTER_MEMBER
import generated.jooq.tables.references.SESSIONS
import generated.jooq.tables.references.SIGN_UP_INQUIRIES
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toBase64
import nl.clicqo.ext.toUUID
import nl.clicqo.messaging.email.EmailComposer
import nl.clicqo.messaging.email.EmailMessagingVerticle
import nl.clicqo.messaging.email.Recipients
import org.jooq.postgres.extensions.types.Inet
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewAccess
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.data.CrewCharterMemberDataVerticle
import run.galley.cloud.data.CrewDataVerticle
import run.galley.cloud.data.SessionDataVerticle
import run.galley.cloud.data.UserDataVerticle
import run.galley.cloud.data.VesselDataVerticle
import run.galley.cloud.model.VesselCrewAccess
import run.galley.cloud.web.JWT
import run.galley.cloud.web.issueAccessToken
import run.galley.cloud.web.issueRefreshToken
import java.net.InetAddress
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class AuthControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val SIGN_UP = "auth.cmd.create"
    const val ACCOUNT_ACTIVATION = "auth.account.cmd.grant"
    const val SIGN_IN = "auth.cmd.issue"
    const val SIGN_OUT = "auth.cmd.revoke"
    const val ISSUE_REFRESH_TOKEN = "auth.refreshToken.cmd.issue"
    const val ISSUE_ACCESS_TOKEN = "auth.accessToken.cmd.issue"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(SIGN_UP, handler = ::signUp)
      vertx.eventBus().coConsumer(ACCOUNT_ACTIVATION, handler = ::accountActivation)
      vertx.eventBus().coConsumer(SIGN_IN, handler = ::signIn)
      vertx.eventBus().coConsumer(SIGN_OUT, handler = ::signOut)
      vertx.eventBus().coConsumer(ISSUE_REFRESH_TOKEN, handler = ::issueRefreshToken)
      vertx.eventBus().coConsumer(ISSUE_ACCESS_TOKEN, handler = ::issueAccessToken)
    }
  }

  private suspend fun getUser(apiRequest: EventBusApiRequest): Users {
    val refreshToken =
      apiRequest.body?.getString("refreshToken") ?: throw ApiStatusReplyException(ApiStatus.REFRESH_TOKEN_MISSING)

    val refreshTokenUser =
      try {
        JWT.authProvider(vertx, config).authenticate(TokenCredentials(refreshToken)).coAwait()
          ?: throw ApiStatusReplyException(ApiStatus.REFRESH_TOKEN_INVALID)
      } catch (_: Exception) {
        throw ApiStatusReplyException(ApiStatus.REFRESH_TOKEN_INVALID)
      }

    val userId = refreshTokenUser.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.REFRESH_TOKEN_INVALID)

    return vertx
      .eventBus()
      .request<EventBusDataResponse<Users>>(
        UserDataVerticle.GET,
        EventBusQueryDataRequest(
          identifiers = mapOf("id" to userId.toString()),
        ),
      ).coAwait()
      ?.body()
      ?.payload
      ?.toOne()
      ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)
  }

  private suspend fun issueRefreshToken(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val user = getUser(apiRequest)
    val refreshToken =
      apiRequest.body?.getString("refreshToken") ?: throw ApiStatusReplyException(ApiStatus.REFRESH_TOKEN_MISSING)

    vertx
      .eventBus()
      .request<EventBusDataResponse<Crew>>(
        CrewDataVerticle.LIST_BY_ACTIVE_USER,
        EventBusQueryDataRequest(
          filters =
            filters {
              CREW.USER_ID eq (user.id ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND))
            },
        ),
      ).coAwait()
      ?.body()
      ?.payload
      ?.toMany()
      ?.ifEmpty { throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER) }
      ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER)

    val newToken =
      JWT
        .authProvider(vertx, config)
        .issueRefreshToken(
          user.id!!,
        )

    val oldTokenHash =
      JWT
        .hashRefreshToken(refreshToken, config)

    val newTokenHash =
      JWT
        .hashRefreshToken(newToken, config)

    val jwtToken = JWT.authProvider(vertx, config).authenticate(TokenCredentials(newToken)).coAwait()

    vertx
      .eventBus()
      .request<EventBusDataResponse<Sessions>>(
        SessionDataVerticle.UPDATE,
        EventBusCmdDataRequest(
          payload =
            JsonObject()
              .put(SESSIONS.REFRESH_TOKEN_HASH.name, newTokenHash)
              .put(SESSIONS.USER_AGENT.name, apiRequest.context.userAgent)
              .put(SESSIONS.IP_ADDRESS.name, apiRequest.context.remoteIp)
              .put(
                SESSIONS.EXPIRES_AT.name,
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(jwtToken.attributes().getLong("exp")), ZoneOffset.UTC).toString(),
              ),
          userId = user.id,
          filters =
            filters {
              SESSIONS.REFRESH_TOKEN_HASH eq oldTokenHash
              SESSIONS.USER_ID eq user.id
            },
        ),
      ).coAwait()
      .body()
      ?.payload
      ?.toOne()

    message.reply(
      EventBusApiResponse(JsonObject().put("refreshToken", newToken)),
    )
  }

  /**
   * Currently the JWT claims of accessToken are the same as the claims of refreshToken.
   * In the future the Access Token will get more info like the user's name and email etc.
   * These are things the refreshToken will not contain since the access tokens are short-lived.
   */
  private suspend fun issueAccessToken(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val user = getUser(apiRequest)

    val vesselCrewResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Crew>>(
          CrewDataVerticle.LIST_BY_USER,
          EventBusQueryDataRequest(
            filters =
              filters {
                CREW.USER_ID eq (user.id ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND))
              },
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toMany()
        ?.ifEmpty { throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER) }
        ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER)

    val crewMemberIds = mutableMapOf<UUID, UUID>()
    val crewAccess = mutableListOf<CrewAccess>()
    val embarking = vesselCrewResponse.size == 1 && vesselCrewResponse.first().status == MemberStatus.invited

    if (embarking && vesselCrewResponse.first().createdAt?.isBefore(OffsetDateTime.now().minusHours(6)) == true) {
      throw ApiStatusReplyException(ApiStatus.CREW_EMBARKING_TOO_OLD)
    }

    vesselCrewResponse
      .forEach {
        if (it.vesselRole == VesselRole.captain) {
          crewAccess.add(VesselCrewAccess(it.vesselId!!, CrewRole.VESSEL_CAPTAIN))
        } else {
          val id = it.id ?: throw ApiStatusReplyException(ApiStatus.ID_MISSING)
          crewMemberIds[id] = it.vesselId!!
        }
      }

    if (crewMemberIds.isNotEmpty()) {
      vertx
        .eventBus()
        .request<EventBusDataResponse<CrewCharterMember>>(
          CrewCharterMemberDataVerticle.LIST_BY_USER,
          EventBusQueryDataRequest(
            filters =
              filters {
                CREW_CHARTER_MEMBER.CREW_ID isIn crewMemberIds.keys
              },
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toMany()
    } else {
      listOf()
    }?.forEach {
      val role = it.charterRole?.name?.uppercase()
      if (role != null) {
        crewAccess.add(
          CharterCrewAccess(
            crewMemberIds[it.crewId] ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND),
            it.charterId!!,
            CrewRole.valueOf("CHARTER_$role"),
          ),
        )
      }
    }

    if (crewAccess.isEmpty()) {
      throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER)
    }

    val newToken =
      JWT.authProvider(vertx, config).issueAccessToken(
        user.id!!,
        crewAccess,
        JsonObject().put("embarking", embarking),
      )

    message.reply(
      EventBusApiResponse(
        JsonObject().put("accessToken", newToken),
      ),
    )
  }

  private suspend fun signIn(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val email = apiRequest.body?.getString("email") ?: throw ApiStatusReplyException(ApiStatus.ID_MISSING)

    val user =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Users>>(
          UserDataVerticle.GET_BY_EMAIL,
          EventBusQueryDataRequest(
            filters =
              filters {
                USERS.EMAIL eq email
              },
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val crewMemberships =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Crew>>(
          CrewDataVerticle.LIST_BY_ACTIVE_USER,
          EventBusQueryDataRequest(
            filters =
              filters {
                CREW.USER_ID eq (user.id ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND))
              },
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toMany()

    val crewMembership = crewMemberships?.firstOrNull() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND)

    when (crewMembership.vesselRole) {
      VesselRole.captain -> CrewRole.VESSEL_CAPTAIN
      else -> TODO("Currently not supported, need to get crew_charter_member info to get charter role")
    }

    val refreshToken = JWT.authProvider(vertx, config).issueRefreshToken(user.id!!)

    val newTokenHash =
      JWT
        .hashRefreshToken(refreshToken, config)

    val jwtToken = JWT.authProvider(vertx, config).authenticate(TokenCredentials(refreshToken)).coAwait()

    vertx
      .eventBus()
      .request<EventBusDataResponse<Sessions>>(
        SessionDataVerticle.CREATE,
        EventBusCmdDataRequest(
          payload =
            JsonObject()
              .put(SESSIONS.REFRESH_TOKEN_HASH.name, newTokenHash)
              .put(SESSIONS.USER_AGENT.name, apiRequest.context.userAgent)
              .put(SESSIONS.IP_ADDRESS.name, apiRequest.context.remoteIp)
              .put(
                SESSIONS.EXPIRES_AT.name,
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(jwtToken.attributes().getLong("exp")), ZoneOffset.UTC).toString(),
              ),
          userId = user.id,
        ),
      ).coAwait()
      .body()
      ?.payload
      ?.toOne()

    message.reply(
      EventBusApiResponse(
        JsonObject().put("refreshToken", refreshToken),
      ),
    )
  }

  private suspend fun signOut(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    vertx
      .eventBus()
      .request<EventBusDataResponse<Sessions>>(
        SessionDataVerticle.REVOKE,
        EventBusCmdDataRequest(
          userId = userId,
        ),
      ).coAwait()
      .body()

    message.reply(EventBusApiResponse())
  }

  private suspend fun signUp(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val intentReq =
      apiRequest.body?.getString("intent") ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_INTENT_MISSING)
    val userReq =
      apiRequest.body.getJsonObject("user") ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_USER_OBJ_MISSING)
    val inquiryReq =
      apiRequest.body.getJsonObject("inquiry") ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_INQUIRY_OBJ_MISSING)
    val vesselReq =
      apiRequest.body.getJsonObject("vessel") ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_VESSEL_OBJ_MISSING)

    val user =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Users>>(
          UserDataVerticle.CREATE,
          EventBusCmdDataRequest(
            userReq,
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vessel =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Vessels>>(
          VesselDataVerticle.CREATE,
          EventBusCmdDataRequest(
            vesselReq,
            userId = user.id,
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND)

    val crew =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Crew>>(
          CrewDataVerticle.CREATE,
          EventBusCmdDataRequest(
            JsonObject()
              .put(CREW.VESSEL_ID.name, vessel.id)
              .put(CREW.VESSEL_ROLE.name, VesselRole.captain),
            userId = user.id,
          ),
        ).coAwait()
        .body()
        ?.payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_CAPTAIN)

    val emailRequest =
      EmailComposer(
        to = Recipients.one(user.email ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)),
        subject = "Activate your account",
        template = "onboarding/activation",
        variables =
          JsonObject().put(
            "activation_url",
            "/auth/activate/${"${crew.id}.${vessel.id}.${user.id}.${crew.activationSalt}".toBase64()}",
          ),
      )

    vertx
      .eventBus()
      .request<JsonObject>(EmailMessagingVerticle.SEND, emailRequest)
      .coAwait()

    vertx
      .eventBus()
      .request<EventBusDataResponse<Users>>(
        UserDataVerticle.SIGN_UP_INQUIRY,
        EventBusCmdDataRequest(
          JsonObject()
            .put(SIGN_UP_INQUIRIES.VESSEL_ID.name, vessel.id)
            .put(SIGN_UP_INQUIRIES.INTENT.name, intentReq)
            .put(SIGN_UP_INQUIRIES.TECHNICAL_EXPERIENCE.name, inquiryReq.getString("technicalExperience"))
            .put(SIGN_UP_INQUIRIES.QUESTIONS.name, inquiryReq),
          userId = user.id,
        ),
      ).coAwait()

    val newToken =
      JWT.authProvider(vertx, config).issueRefreshToken(
        user.id!!,
      )

    message.reply(
      EventBusApiResponse(JsonObject().put("refreshToken", newToken)),
    )
  }

  private suspend fun accountActivation(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()

    val userId =
      apiRequest.body?.getString("userId")?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)
    val vesselId =
      apiRequest.body.getString("vesselId")?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND)
    val crewId =
      apiRequest.body.getString("crewId")?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER)
    val activationSalt =
      apiRequest.body.getString("activationSalt") ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_ACTIVATION_SALT)

    vertx
      .eventBus()
      .request<EventBusDataResponse<Crew>>(
        CrewDataVerticle.ACTIVATE,
        EventBusQueryDataRequest(
          filters =
            filters {
              CREW.ID eq crewId
              CREW.USER_ID eq userId
              CREW.VESSEL_ID eq vesselId
              CREW.ACTIVATION_SALT eq activationSalt
            },
        ),
      ).coAwait()
      .body()
      ?.payload
      ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_CAPTAIN)

    message.reply(EventBusApiResponse())
  }
}
