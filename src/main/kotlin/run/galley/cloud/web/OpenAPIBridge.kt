package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.contract.OpenAPIContract
import io.vertx.openapi.validation.ValidatedRequest
import nl.clicqo.api.APIResponse
import nl.clicqo.api.APIResponseOptions
import nl.clicqo.api.OpenAPIBridgeRouter
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.fromEventBusDataResponse
import nl.kleilokaal.queue.modules.addCoroutineHandler

class OpenAPIBridge(val config: JsonObject) : OpenAPIBridgeRouter() {
  override suspend fun buildRouter(vertx: Vertx): Router {
    val openApiFile = config.getJsonObject("api").getString("openapiFile", "openapi.yaml")
    val contract = OpenAPIContract.from(vertx, openApiFile).coAwait()
    val openAPIRouter = RouterBuilder.create(vertx, contract)

    val authConfig = JWTAuthOptions()
      .setKeyStore(
        KeyStoreOptions()
          .setType(config.getJsonObject("jwt", JsonObject()).getString("type", "jceks"))
          .setPath(config.getJsonObject("jwt", JsonObject()).getString("keystore", "keystore.jceks"))
          .setPassword(config.getJsonObject("jwt", JsonObject()).getString("secret", ""))
      )

    // @TODO: Delete this line, temp JWT generation for testing
    println(JWTAuth.create(vertx, authConfig).generateToken(JsonObject().put("scope", "vesselCaptain")))

    val authProvider = JWTAuth.create(vertx, authConfig)

    /**
     * Add security handlers for each scope.
     */
    openAPIRouter
      .security("vesselCaptain")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("vesselCaptain"))

      .security("charterCaptain")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("charterCaptain"))

      .security("charterPurser")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("charterPurser"))

      .security("charterBoatswain")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("charterBoatswain"))

      .security("charterDeckhand")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("charterDeckhand"))

      .security("charterSteward")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("charterSteward"))

    /**
     * Add eventbus handlers for each operation.
     */
    openAPIRouter.routes.forEach { route ->
      val operation = route.operation
      val address = createAddress(operation.operationId)

      route.addCoroutineHandler(vertx) { routingContext ->
        catchAll(routingContext) {
          val validatedRequest = routingContext.get<ValidatedRequest>(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST)

          val params = validatedRequest.pathParameters
          val body = validatedRequest.body as? JsonObject
          val query = validatedRequest.query

          /**
           * Currently only supports JSON as a response format.
           */
          val acceptHeader = routingContext.request().getHeader("Accept") ?: "*/*"
          val acceptsJson = acceptHeader.split(",").find { header ->
            header.trim().endsWith("/*", true) ||
              header.trim().endsWith("/json", true) ||
              header.trim().endsWith("+json", true)
          }

          /**
           * Anything other than JSON is not supported at the moment.
           */
          if (acceptsJson.isNullOrBlank()) {
            routingContext.response().setStatusCode(406).end("Not Acceptable")
            return@catchAll
          }

          val eb = routingContext.vertx().eventBus()

          val response = eb.request<EventBusDataResponse>(
            address,
            EventBusDataRequest(
              identifiers = params,
              body = body,
              query = query
//            version = responseVersion,
            )
          ).coAwait().body()

          // TODO: use requested responseFormat to return JSON or something else.
          val apiResponseOptions = APIResponseOptions(contentType = "application/vnd.galley.v1+json")

          APIResponse(
            routingContext,
            apiResponseOptions
          )
            .fromEventBusDataResponse(response)
            .end()
        }
      }
    }

    return openAPIRouter.createRouter()
  }
}
