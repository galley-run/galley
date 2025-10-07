package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject

class EventBusApiResponse(
  val payload: JsonObject? = null,
  val version: String = "v1",
  val format: String = "json"
)
