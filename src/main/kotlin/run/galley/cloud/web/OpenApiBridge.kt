package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.validation.ValidatedRequest
import nl.clicqo.api.ApiResponse
import nl.clicqo.api.ApiResponseOptions
import nl.clicqo.api.OpenAPIBridgeRouter
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.fromEventBusDataResponse
import nl.kleilokaal.queue.modules.addCoroutineHandler

class OpenApiBridge(override val vertx: Vertx, override val config: JsonObject) : OpenAPIBridgeRouter(vertx, config) {
  override suspend fun buildRouter(): RouterBuilder {
    /**
     * Add security handlers for each scope.
     */
    openAPIRouterBuilder
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
    openAPIRouterBuilder.routes.forEach { route ->
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
            .fromEventBusDataResponse(response)
            .end()
        }
      }
    }

    return openAPIRouterBuilder
  }
}
