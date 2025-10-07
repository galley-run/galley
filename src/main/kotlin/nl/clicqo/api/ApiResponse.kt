package nl.clicqo.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.web.HttpStatus

class ApiResponse(
  val routingContext: RoutingContext,
  apiResponseOptions: ApiResponseOptions = ApiResponseOptions()
) {
  var body: JsonObject? = null
  var httpStatus: HttpStatus = HttpStatus.NoContent
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
    this.httpStatus = HttpStatus.Ok
    this.body = eventBusApiResponse.payload

    return this
  }

  fun end() {
    if (httpStatus == HttpStatus.NoContent && body != null) {
      httpStatus = HttpStatus.Ok
    }

    routingContext.response()
      .setStatusCode(httpStatus.code)
      .setStatusMessage(httpStatus.statusMessage)
      .putHeader("content-type", contentType)
    routingContext
      .end(build())
  }
}

