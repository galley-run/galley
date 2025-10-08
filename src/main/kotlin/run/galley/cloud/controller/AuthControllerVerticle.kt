package run.galley.cloud.controller

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.ext.toUUID
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.CrewDataVerticle
import run.galley.cloud.data.UserDataVerticle
import run.galley.cloud.db.generated.enums.VesselRole
import run.galley.cloud.db.generated.tables.pojos.Crew
import run.galley.cloud.db.generated.tables.pojos.Users
import run.galley.cloud.model.UserRole
import run.galley.cloud.web.JWT
import run.galley.cloud.web.getVesselId
import run.galley.cloud.web.issueAccessToken
import run.galley.cloud.web.issueRefreshToken

class AuthControllerVerticle : CoroutineVerticle() {
  companion object {
    const val ADDRESS_ISSUE_REFRESH_TOKEN = "auth.refreshToken.cmd.issue"
    const val ADDRESS_ISSUE_ACCESS_TOKEN = "auth.accessToken.cmd.issue"
    const val ADDRESS_SIGN_IN = "auth.cmd.issue"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_ISSUE_REFRESH_TOKEN, ::issueRefreshToken)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_ISSUE_ACCESS_TOKEN, ::issueAccessToken)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_SIGN_IN, ::signIn)
  }

  private suspend fun issueRefreshToken(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val refreshToken = apiRequest.body?.getString("refreshToken") ?: throw ApiStatus.REFRESH_TOKEN_MISSING

    val refreshTokenUser = JWT.authProvider(vertx, config).authenticate(TokenCredentials(refreshToken)).coAwait()
      ?: throw ApiStatus.REFRESH_TOKEN_INVALID

    val userId = refreshTokenUser.subject()?.toUUID() ?: throw ApiStatus.REFRESH_TOKEN_INVALID
    val vesselId = refreshTokenUser.getVesselId() ?: throw ApiStatus.VESSEL_ID_INCORRECT

    val user = vertx.eventBus().request<EventBusDataResponse<Users>>(
      UserDataVerticle.ADDRESS_GET, EventBusDataRequest(
        identifiers = mapOf("id" to userId.toString()),
      )
    ).coAwait().body().payload.toSingle() ?: throw ApiStatus.USER_NOT_FOUND

    val crewResponse = vertx.eventBus().request<EventBusDataResponse<Crew>>(
      CrewDataVerticle.ADDRESS_GET_BY_USER_AND_VESSEL, EventBusDataRequest(
        identifiers = mapOf(
          "userId" to user.id.toString(),
          "vesselId" to vesselId.toString()
        ),
      )
    ).coAwait().body().payload.toSingle() ?: throw ApiStatus.CREW_NO_VESSEL_MEMBER

    when (crewResponse.vesselRole) {
      VesselRole.captain -> UserRole.VESSEL_CAPTAIN
      else -> TODO("Currently not supported, need to get crew_charter_member info to get charter role")
    }

    // We don't need to add the user role into the refresh token, since the refresh token is only here to create
    // an access token and roles may change over time.

    val newToken = JWT.authProvider(vertx, config).issueRefreshToken(
      user.id!!,
      JWT.claims(vesselId)
    )

    message.reply(
      EventBusApiResponse(
        payload = JsonObject().put(
          "refreshToken", newToken
        )
      )
    )
  }

  /**
   * Currently the JWT claims of accessToken are the same as the claims of refreshToken.
   * In the future the Access Token will get more info like the user's name and email etc.
   * These are things the refreshToken will not contain since the access tokens are short-lived.
   */
  private suspend fun issueAccessToken(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val refreshToken = apiRequest.body?.getString("refreshToken") ?: throw ApiStatus.REFRESH_TOKEN_MISSING

    val refreshTokenUser = JWT.authProvider(vertx, config).authenticate(TokenCredentials(refreshToken)).coAwait()
      ?: throw ApiStatus.REFRESH_TOKEN_INVALID

    val userId = refreshTokenUser.subject()?.toUUID() ?: throw ApiStatus.REFRESH_TOKEN_INVALID
    val vesselId = refreshTokenUser.getVesselId() ?: throw ApiStatus.VESSEL_ID_INCORRECT

    val user = vertx.eventBus().request<EventBusDataResponse<Users>>(
      UserDataVerticle.ADDRESS_GET, EventBusDataRequest(
        identifiers = mapOf("id" to userId.toString()),
      )
    ).coAwait().body().payload.toSingle() ?: throw ApiStatus.USER_NOT_FOUND

    val crewResponse = vertx.eventBus().request<EventBusDataResponse<Crew>>(
      CrewDataVerticle.ADDRESS_GET_BY_USER_AND_VESSEL, EventBusDataRequest(
        identifiers = mapOf(
          "userId" to user.id.toString(),
          "vesselId" to vesselId.toString()
        ),
      )
    ).coAwait().body().payload.toSingle() ?: throw ApiStatus.CREW_NO_VESSEL_MEMBER

    val userRole = when (crewResponse.vesselRole) {
      VesselRole.captain -> UserRole.VESSEL_CAPTAIN
      else -> TODO("Currently not supported, need to get crew_charter_member info to get charter role")
    }

    val newToken = JWT.authProvider(vertx, config).issueAccessToken(
      user.id!!,
      userRole,
      JWT.claims(vesselId)
    )

    message.reply(
      EventBusApiResponse(
        payload = JsonObject().put(
          "accessToken", newToken
        )
      )
    )
  }

  private suspend fun signIn(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val email = apiRequest.body?.getString("email") ?: throw ApiStatus.ID_MISSING

    val user = vertx.eventBus().request<EventBusDataResponse<Users>>(
      UserDataVerticle.ADDRESS_GET_BY_EMAIL, EventBusDataRequest(
        filters = mapOf("email" to listOf(email))
      )
    ).coAwait().body().payload.toSingle() ?: throw ApiStatus.USER_NOT_FOUND

    val crewMemberships = vertx.eventBus().request<EventBusDataResponse<Crew>>(
      CrewDataVerticle.ADDRESS_LIST_ACTIVE, EventBusDataRequest(
        filters = mapOf(
          "userId" to listOf(user.id!!.toString()),
        )
      )
    ).coAwait().body().payload.toMany()

    val crewMembership = crewMemberships?.firstOrNull() ?: throw ApiStatus.VESSEL_NOT_FOUND

    when (crewMembership.vesselRole) {
      VesselRole.captain -> UserRole.VESSEL_CAPTAIN
      else -> TODO("Currently not supported, need to get crew_charter_member info to get charter role")
    }

    message.reply(
      EventBusApiResponse(
        payload = JsonObject().put(
          "refreshToken",
          JWT.authProvider(vertx, config).issueRefreshToken(
            user.id!!,
            JWT.claims(crewMembership.vesselId!!),
          )
        )

      )
    )
  }
}
