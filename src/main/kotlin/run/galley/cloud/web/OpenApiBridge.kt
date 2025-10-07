package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.validation.ValidatedRequest
import nl.clicqo.api.ApiResponse
import nl.clicqo.api.ApiResponseOptions
import nl.clicqo.api.OpenAPIBridgeRouter
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.kleilokaal.queue.modules.addCoroutineHandler

class OpenApiBridge(override val vertx: Vertx, override val config: JsonObject) : OpenAPIBridgeRouter(vertx, config) {
  val logger = LoggerFactory.getLogger(this::class.java)
  override suspend fun buildRouter(): RouterBuilder {
    /**
     * Add security handlers for each scope.
     */
    openAPIRouterBuilder
      .security("vesselCaptain")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("VESSEL_CAPTAIN"))

      .security("charterCaptain")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("CHARTER_CAPTAIN"))

      .security("charterPurser")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("CHARTER_PURSER"))

      .security("charterBoatswain")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("CHARTER_BOATSWAIN"))

      .security("charterDeckhand")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("CHARTER_DECKHAND"))

      .security("charterSteward")
      .httpHandler(JWTAuthHandler.create(authProvider).withScope("CHARTER_STEWARD"))

    /**
     * Add eventbus handlers for each operation.
     */
    openAPIRouterBuilder.routes.forEach { route ->
      val operation = route.operation
      // Here we'll remove the prefix (if any) of the eventbus address to avoid direct access into Data Verticles
      val address = operation.operationId.removePrefix("data.")
      logger.info("Registered eventbus address: $address")

      route.addCoroutineHandler(vertx) { routingContext ->
        catchAll(routingContext) {
          val validatedRequest = routingContext.get<ValidatedRequest>(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST)

          val params = validatedRequest.pathParameters
          val body = validatedRequest.body.get() as? JsonObject
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

          val response = eb.request<EventBusApiResponse>(
            address,
            EventBusApiRequest(
              user = routingContext.user(),
              identifiers = params,
              body = body,
              query = query
//            version = responseVersion,
            )
          ).coAwait().body()

          // TODO: use requested responseFormat to return JSON or something else.
          val apiResponseOptions = ApiResponseOptions(contentType = "application/vnd.galley.v1+json")

          ApiResponse(
            routingContext,
            apiResponseOptions
          )
            .fromEventBusApiResponse(response)
            .end()
        }
      }
    }

    return openAPIRouterBuilder
  }
}
