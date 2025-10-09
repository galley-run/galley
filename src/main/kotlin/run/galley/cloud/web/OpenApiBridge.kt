package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.validation.ResponseValidator
import io.vertx.openapi.validation.ValidatedRequest
import nl.clicqo.api.ApiResponse
import nl.clicqo.api.ApiResponseOptions
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.OpenAPIBridgeRouter
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.kleilokaal.queue.modules.addCoroutineHandler

class OpenApiBridge(
  override val vertx: Vertx,
  override val config: JsonObject,
) : OpenAPIBridgeRouter(vertx, config) {
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
          val rawBody = validatedRequest.body.jsonObject
          val query = validatedRequest.query

          val contractOperation = openAPIContract.operation(operation.operationId)

          /**
           * Currently only supports JSON as a response format.
           */
          val acceptHeader = routingContext.request().getHeader("Accept") ?: "*/*"
          val acceptsJson =
            acceptHeader.split(",").find { header ->
              header.trim().endsWith("/*", true) ||
                header.trim().endsWith("/json", true) ||
                header.trim().endsWith("+json", true)
            }

          val requestedVersion =
            acceptHeader
              .takeIf { it.startsWith("application/vnd.") }
              ?.substringAfterLast(".")
              ?.split("+")
              ?.firstOrNull() ?: "v1"
          val requestedFormat =
            acceptHeader
              .takeIf { it.startsWith("application/vnd.") }
              ?.substringAfterLast("+") ?: "json"

          /**
           * Anything other than JSON is not supported at the moment.
           */
          if (acceptsJson.isNullOrBlank()) {
            routingContext.response().setStatusCode(406).end("Not Acceptable")
            return@catchAll
          }

          /**
           * Filter body to only include keys defined in the OpenAPI contract
           */
          val requestBodySchema =
            contractOperation.requestBody
              ?.content
              ?.get(acceptHeader)
              ?.schema

          val requiredProperties =
            requestBodySchema
              ?.get<JsonArray>("required")
              ?.toList() ?: emptyList()
          val properties =
            requestBodySchema
              ?.get<JsonObject>("properties")
              ?.fieldNames()
              ?.toList() ?: emptyList()

          val body =
            if (rawBody != null) {
              val filteredBody = rawBody.map.filterKeys { properties.contains(it) }

              // Check if all required properties are present
              val missingRequired = requiredProperties.filterNot { filteredBody.containsKey(it) }
              if (missingRequired.isNotEmpty()) {
                throw ApiStatus.REQUEST_BODY_MISSING_REQUIRED_FIELDS
              }

              JsonObject(filteredBody)
            } else {
              null
            }

          val response =
            routingContext
              .vertx()
              .eventBus()
              .request<EventBusApiResponse>(
                address,
                EventBusApiRequest(
                  user = routingContext.user(),
                  identifiers = params,
                  body = body,
                  query = query,
                  format = requestedFormat,
                  version = requestedVersion,
                ),
              ).coAwait()
              .body()

          val apiResponseOptions =
            ApiResponseOptions(contentType = "application/vnd.galley.$requestedVersion+$requestedFormat")

          ApiResponse(
            routingContext,
            apiResponseOptions,
          ).fromEventBusApiResponse(response)
            .end(ResponseValidator.create(vertx, openAPIContract), operation.operationId, openAPIContract)
        }
      }
    }

    return openAPIRouterBuilder
  }
}
