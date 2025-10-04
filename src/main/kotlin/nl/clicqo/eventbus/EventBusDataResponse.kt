package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import nl.clicqo.api.APIResponse
import nl.clicqo.web.HttpStatus

class EventBusDataResponse(
  val payload: JsonObject? = null,
  val version: String = "v1",
  val format: String = "json"
)

fun APIResponse.fromEventBusDataResponse(eventBusDataResponse: EventBusDataResponse): APIResponse {
  this.httpStatus = HttpStatus.Ok
  this.body = eventBusDataResponse.payload

  return this
}
