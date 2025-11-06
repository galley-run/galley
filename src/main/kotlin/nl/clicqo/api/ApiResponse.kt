package nl.clicqo.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.json.schema.JsonSchemaValidationException
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.contract.OpenAPIContract
import io.vertx.openapi.validation.ResponseValidator
import io.vertx.openapi.validation.ValidatableResponse
import io.vertx.openapi.validation.ValidatorException
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.ext.applyIf
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

  fun addError(
    error: ApiStatus,
    source: ApiErrorSource? = null,
  ): ApiResponse {
    if (errors == null) {
      errors = JsonArray()
    }
    errors?.add(
      JsonObject()
        .put("status", error.code)
        .put("title", error.message)
        .applyIf(source != null) {
          this.put("source", source?.toJsonObject())
          this
        },
    )
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
    this.httpStatus = eventBusApiResponse.httpStatus

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

    if (httpStatus == HttpStatus.NoContent && eventBusApiResponse.data != null) {
      httpStatus = HttpStatus.Ok
    }
    if (httpStatus == HttpStatus.Ok && eventBusApiResponse.data == null) {
      httpStatus = HttpStatus.NoContent
      body = null
    }
    if (httpStatus.code in 200..<300 && eventBusApiResponse.errors != null) {
      httpStatus = HttpStatus.InternalServerError
    }

    return this
  }

  fun end() {
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
    routingContext
      .response()
      .setStatusCode(httpStatus.code)
      .setStatusMessage(httpStatus.statusMessage)
      .putHeader("content-type", contentType)

    // Filter body data based on OpenAPI schema
    var filteredBody =
      body?.let { bodyObj ->
        val data = bodyObj.getValue("data")
        val filteredData = OpenAPISchemaFilter.filterBySchema(data, contract, operationId, httpStatus.code, contentType)

        JsonObject(bodyObj.map).apply {
          if (filteredData != null) {
            put("data", filteredData)
          }
        }
      }

    if (httpStatus === HttpStatus.NoContent) {
      filteredBody = null
    }

    val response = ValidatableResponse.create(httpStatus.code, filteredBody?.toBuffer(), contentType)
    try {
      val validatedResponse = responseValidator.validate(response, operationId).coAwait()
      validatedResponse.send(routingContext.response())
    } catch (e: JsonSchemaValidationException) {
      logger.error("203 - SCHEMA VALIDATION ERROR", e)
      logger.error(e.message)
      routingContext.response().statusCode = HttpStatus.NonAuthoritativeInformation.code
      routingContext.response().end(filteredBody?.toBuffer())
    } catch (e: ValidatorException) {
      logger.error("203 - VALIDATOR ERROR", e)
      logger.error(e.message)
      routingContext.response().statusCode = HttpStatus.NonAuthoritativeInformation.code
      routingContext.response().end(filteredBody?.toBuffer())
    }
  }
}
