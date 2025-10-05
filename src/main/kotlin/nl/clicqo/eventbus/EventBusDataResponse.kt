package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiResponse
import nl.clicqo.web.HttpStatus

class EventBusDataResponse(
  val payload: JsonObject? = null,
  val version: String = "v1",
  val format: String = "json"
)

fun ApiResponse.fromEventBusDataResponse(eventBusDataResponse: EventBusDataResponse): ApiResponse {
  this.httpStatus = HttpStatus.Ok
  this.body = eventBusDataResponse.payload

  return this
}
