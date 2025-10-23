package run.galley.cloud.controller

import generated.jooq.enums.VesselRole
import generated.jooq.tables.Crew.Companion.CREW
import generated.jooq.tables.Users.Companion.USERS
import generated.jooq.tables.pojos.Crew
import generated.jooq.tables.pojos.CrewCharterMember
import generated.jooq.tables.pojos.Users
import generated.jooq.tables.references.CREW_CHARTER_MEMBER
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import org.jooq.impl.DSL.user
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewAccess
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.data.CrewCharterMemberDataVerticle
import run.galley.cloud.data.CrewDataVerticle
import run.galley.cloud.data.UserDataVerticle
import run.galley.cloud.model.VesselCrewAccess
import run.galley.cloud.web.JWT
import run.galley.cloud.web.issueAccessToken
import run.galley.cloud.web.issueRefreshToken
import java.util.UUID

class AuthControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val ISSUE_REFRESH_TOKEN = "auth.refreshToken.cmd.issue"
    const val ISSUE_ACCESS_TOKEN = "auth.accessToken.cmd.issue"
    const val SIGN_IN = "auth.cmd.issue"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(ISSUE_REFRESH_TOKEN, handler = ::issueRefreshToken)
      vertx.eventBus().coConsumer(ISSUE_ACCESS_TOKEN, handler = ::issueAccessToken)
      vertx.eventBus().coConsumer(SIGN_IN, handler = ::signIn)
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
        ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER)

    val crewMemberIds = mutableMapOf<UUID, UUID>()
    val crewAccess = mutableListOf<CrewAccess>()
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
          CrewDataVerticle.LIST_ACTIVE,
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
}
