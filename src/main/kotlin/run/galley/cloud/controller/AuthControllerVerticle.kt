package run.galley.cloud.controller

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.UserRole
import run.galley.cloud.web.JWT
import run.galley.cloud.web.issueAccessToken
import run.galley.cloud.web.issueRefreshToken

class AuthControllerVerticle : CoroutineVerticle() {
  companion object {
    const val ADDRESS_ISSUE_REFRESH_TOKEN = "auth.refreshToken.cmd.issue"
    const val ADDRESS_ISSUE_ACCESS_TOKEN = "auth.accessToken.cmd.issue"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_ISSUE_REFRESH_TOKEN, ::issueRefreshToken)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_ISSUE_ACCESS_TOKEN, ::issueAccessToken)
  }

  private suspend fun issueRefreshToken(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val refreshToken = apiRequest.body?.getString("refreshToken") ?: throw ApiStatus.REFRESH_TOKEN_INVALID

    // Check if refresh token is valid

    // Generate new refresh token and return it
    val authProvider = JWT.authProvider(vertx, config)

    message.reply(
      EventBusApiResponse(
        payload = JsonObject().put(
          "refreshToken", authProvider.issueRefreshToken(
            "5d4fbdef-21fa-4cff-8d20-b6c74d55b9c0",
            UserRole.VESSEL_CAPTAIN
          )
        )
      )
    )
  }

  private suspend fun issueAccessToken(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val refreshToken = apiRequest.body?.getString("refreshToken") ?: throw ApiStatus.REFRESH_TOKEN_INVALID

    // Check if refresh token is valid

    // Generate new refresh token and return it
    val authProvider = JWT.authProvider(vertx, config)

    message.reply(
      EventBusApiResponse(
        payload = JsonObject().put(
          "accessToken", authProvider.issueAccessToken(
            "5d4fbdef-21fa-4cff-8d20-b6c74d55b9c0",
            UserRole.VESSEL_CAPTAIN
          )
        )
      )
    )
  }
}
