package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.validation.ResponseValidator
import io.vertx.openapi.validation.ValidatedRequest
import nl.clicqo.api.ApiResponse
import nl.clicqo.api.ApiResponseOptions
import nl.clicqo.api.OpenAPIBridgeRouter
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiRequestContext
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.ext.addCoroutineHandler
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.crew.getCrewRole
import run.galley.cloud.crew.getVessels

class OpenApiBridge(
  override val vertx: Vertx,
  override val config: JsonObject,
) : OpenAPIBridgeRouter(vertx, config) {
  val logger = LoggerFactory.getLogger(this::class.java)

  override suspend fun buildRouter(): RouterBuilder {
//    /**
//     * Add security handlers for each scope.
//     */
    val scpAuthHandler = JWTAuthHandlerScp(authProvider)

    openAPIRouterBuilder
      .security("charterSteward")
      .httpHandler(scpAuthHandler)
      .security("charterDeckhand")
      .httpHandler(scpAuthHandler)
      .security("charterBoatswain")
      .httpHandler(scpAuthHandler)
      .security("charterPurser")
      .httpHandler(scpAuthHandler)
      .security("charterCaptain")
      .httpHandler(scpAuthHandler)
      .security("vesselCaptain")
      .httpHandler(scpAuthHandler)
      .security("any")
      .httpHandler(scpAuthHandler)

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
          // Check if user is authenticated
          val validatedRequest = routingContext.get<ValidatedRequest>(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST)

          val params = validatedRequest.pathParameters

          // If user is not authenticated -> continue
          if (routingContext.user() == null) {
            routingContext.next()
            return@catchAll
          }

          routingContext.put("vesselId", routingContext.user().principal().getString("vesselId"))

          if (params.contains("vesselId")) {
            val requestedVesselId = params["vesselId"]?.string?.toUUID() ?: throw ApiStatus.VESSEL_ID_INCORRECT
            val crewRole = requestedVesselId.let(routingContext.user()::getCrewRole)

            // Check if vesselId in JWT matches the requested Vessel ID
            if (routingContext.user().getVessels()?.contains(requestedVesselId) == false) {
              throw ApiStatus.CREW_NO_VESSEL_MEMBER
            } else if (!params.contains("charterId")) {
              // Allow it
              routingContext.put("crewRole", crewRole)
              routingContext.next()
              return@catchAll
            }

            // Check if user is vessel captain of this vessel
            if (crewRole != null && crewRole == CrewRole.VESSEL_CAPTAIN) {
              // All good - user is captain of their own vessel
              routingContext.put("crewRole", crewRole)
              routingContext.next()
              return@catchAll
            }

            if (params.contains("charterId")) {
              val requestedCharterId = params["charterId"]?.string?.toUUID() ?: throw ApiStatus.CHARTER_ID_INCORRECT

              // Check if user has access to the charter (charter ids should be in JWT, added from table crew_charter_member)
              val crewRole =
                routingContext.user().getCrewRole(requestedVesselId, requestedCharterId)
                  ?: throw ApiStatus.CREW_NO_CHARTER_MEMBER

              routingContext.put("crewRole", crewRole)
              routingContext.next()
              return@catchAll
            }
          }

          // TODO: Possibly add crew_project_member later? Don't see big benefits for it now..

          routingContext.next()
        }
      }

      route.addCoroutineHandler(vertx) { routingContext ->
        catchAll(routingContext) {
          val validatedRequest = routingContext.get<ValidatedRequest>(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST)

          val pathParams = validatedRequest.pathParameters
          val rawBody = validatedRequest.body.jsonObject
          val query = validatedRequest.query

          val contractOperation = openAPIContract.operation(operation.operationId)

          /**
           * Currently only supports JSON as a response format.
           */
          val contentType =
            routingContext
              .request()
              .getHeader("Content-Type")
              ?.substringBefore(";")
              ?.trim()
              ?.takeIf(String::isNotEmpty) ?: "*/*"
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
              ?.get(contentType)
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
                throw nl.clicqo.api.ApiStatus.REQUEST_BODY_MISSING_REQUIRED_FIELDS
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
                  pathParams = pathParams,
                  crewRole = routingContext.get<CrewRole?>("crewRole"),
                  body = body,
                  query = query,
                  format = requestedFormat,
                  version = requestedVersion,
                  context =
                    EventBusApiRequestContext(
                      userAgent = routingContext.request().getHeader("User-Agent"),
                      remoteIp =
                        routingContext
                          .request()
                          .connection()
                          .remoteAddress()
                          .hostAddress(),
                    ),
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
