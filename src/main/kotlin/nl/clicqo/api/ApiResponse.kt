package nl.clicqo.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.contract.OpenAPIContract
import io.vertx.openapi.validation.ResponseValidator
import io.vertx.openapi.validation.ValidatableResponse
import io.vertx.openapi.validation.ValidatorException
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.web.HttpStatus
import org.slf4j.LoggerFactory

class ApiResponse(
  val routingContext: RoutingContext,
  apiResponseOptions: ApiResponseOptions = ApiResponseOptions(),
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  var body: JsonObject? = null
  var httpStatus: HttpStatus = apiResponseOptions.httpStatus
  var contentType: String = apiResponseOptions.contentType
  var errors: JsonArray? = null

  fun setHttpStatus(httpStatus: HttpStatus): ApiResponse {
    this.httpStatus = httpStatus
    return this
  }

  fun addError(error: ApiStatus): ApiResponse {
    if (errors == null) {
      errors = JsonArray()
    }
    errors?.add(JsonObject().put("code", error.code).put("message", error.message))
    setHttpStatus(error.httpStatus)

    return this
  }

  private fun build(): String? {
    if (errors != null) {
      return JsonObject().put("errors", errors).encode()
    }
    return (body ?: JsonObject()).encode()
  }

  fun fromEventBusApiResponse(eventBusApiResponse: EventBusApiResponse): ApiResponse {
    this.httpStatus = eventBusApiResponse.httpStatus ?: HttpStatus.Ok
    this.contentType =
      "application/vnd.galley.${eventBusApiResponse.version}+${eventBusApiResponse.format}"
    this.body =
      JsonObject()
        .put(
          "data",
          eventBusApiResponse.data,
        ).run {
          eventBusApiResponse.meta?.let { this.put("meta", it) }
          eventBusApiResponse.links?.let { this.put("links", it) }
          eventBusApiResponse.included?.let { this.put("included", it) }
          eventBusApiResponse.errors?.let {
            this.remove("data")
            this.remove("included")
            this.put("errors", it)
          }

          this
        }

    return this
  }

  fun end() {
    if (httpStatus == HttpStatus.NoContent && body != null) {
      httpStatus = HttpStatus.Ok
    }

    routingContext
      .response()
      .setStatusCode(httpStatus.code)
      .setStatusMessage(httpStatus.statusMessage)
      .putHeader("content-type", contentType)

    routingContext
      .end(build())
  }

  suspend fun end(
    responseValidator: ResponseValidator,
    operationId: String,
    contract: OpenAPIContract,
  ) {
    if (httpStatus == HttpStatus.NoContent && body != null) {
      httpStatus = HttpStatus.Ok
    }

    routingContext
      .response()
      .setStatusCode(httpStatus.code)
      .setStatusMessage(httpStatus.statusMessage)
      .putHeader("content-type", contentType)

    // Filter body data based on OpenAPI schema
    val filteredBody =
      body?.let { bodyObj ->
        val data = bodyObj.getValue("data")
        val filteredData = OpenAPISchemaFilter.filterBySchema(data, contract, operationId, httpStatus.code, contentType)

        JsonObject(bodyObj.map).apply {
          if (filteredData != null) {
            put("data", filteredData)
          }
        }
      }

    val response = ValidatableResponse.create(httpStatus.code, filteredBody?.toBuffer(), contentType)
    val validatedResponse =
      try {
        responseValidator.validate(response, operationId).coAwait()
      } catch (e: ValidatorException) {
        logger.error(e.message)
        throw ApiStatus.RESPONSE_VALIDATION_FAILED
      }
    validatedResponse.send(routingContext.response())
  }
}
