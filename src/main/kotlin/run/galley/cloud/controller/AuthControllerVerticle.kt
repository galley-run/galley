package run.galley.cloud.controller

import generated.jooq.enums.MemberStatus
import generated.jooq.enums.VesselRole
import generated.jooq.tables.Crew.Companion.CREW
import generated.jooq.tables.Users.Companion.USERS
import generated.jooq.tables.pojos.Crew
import generated.jooq.tables.pojos.CrewCharterMember
import generated.jooq.tables.pojos.Users
import generated.jooq.tables.pojos.Vessels
import generated.jooq.tables.references.CREW_CHARTER_MEMBER
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
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewAccess
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.data.CrewCharterMemberDataVerticle
import run.galley.cloud.data.CrewDataVerticle
import run.galley.cloud.data.UserDataVerticle
import run.galley.cloud.data.VesselDataVerticle
import run.galley.cloud.model.VesselCrewAccess
import run.galley.cloud.web.JWT
import run.galley.cloud.web.issueAccessToken
import run.galley.cloud.web.issueRefreshToken
import java.time.OffsetDateTime
import java.util.UUID

class AuthControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val ISSUE_REFRESH_TOKEN = "auth.refreshToken.cmd.issue"
    const val ISSUE_ACCESS_TOKEN = "auth.accessToken.cmd.issue"
    const val SIGN_IN = "auth.cmd.issue"
    const val SIGN_UP = "auth.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(ISSUE_REFRESH_TOKEN, handler = ::issueRefreshToken)
      vertx.eventBus().coConsumer(ISSUE_ACCESS_TOKEN, handler = ::issueAccessToken)
      vertx.eventBus().coConsumer(SIGN_IN, handler = ::signIn)
      vertx.eventBus().coConsumer(SIGN_UP, handler = ::signUp)
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
      JWT.authProvider(vertx, config).issueRefreshToken(
        user.id!!,
      )

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

    message.reply(
      EventBusApiResponse(
        JsonObject().put("refreshToken", JWT.authProvider(vertx, config).issueRefreshToken(user.id!!)),
      ),
    )
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
//    val charterReq = apiRequest.body.getJsonObject("charter") ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_CHARTER_OBJ_MISSING)
//    val projectReq = apiRequest.body.getJsonObject("project") ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_PROJECT_OBJ_MISSING)
//    val vesselBillingProfileReq = apiRequest.body.getJsonObject("vesselBillingProfile")

    val user =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Users>>(
          UserDataVerticle.CREATE,
          EventBusCmdDataRequest(
            userReq.keysToSnakeCase(),
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
            vesselReq.keysToSnakeCase(),
            userId = user.id,
          ),
        ).coAwait()
        ?.body()
        ?.payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND)

    vertx.eventBus().request<EventBusDataResponse<Crew>>(
      CrewDataVerticle.CREATE,
      EventBusCmdDataRequest(
        JsonObject()
          .put(CREW.VESSEL_ID.name, vessel.id)
          .put(CREW.VESSEL_ROLE.name, VesselRole.captain),
        userId = user.id,
      ),
    )

    // TODO: Send email to activate vessel crew membership, by not activating directly it avoids next login without confirming

    vertx
      .eventBus()
      .request<EventBusDataResponse<Users>>(
        UserDataVerticle.SIGN_UP_INQUIRY,
        EventBusCmdDataRequest(
          JsonObject()
            .put(SIGN_UP_INQUIRIES.VESSEL_ID.name, vessel.id)
            .put(SIGN_UP_INQUIRIES.INTENT.name, intentReq)
            .put(SIGN_UP_INQUIRIES.TECHNICAL_EXPERIENCE.name, inquiryReq.getString("technicalExperience"))
            .put(SIGN_UP_INQUIRIES.QUESTIONS.name, inquiryReq.keysToSnakeCase()),
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
}
