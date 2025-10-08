package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject

data class EventBusApiResponse(
  val payload: JsonObject,
  val version: String = "v1",
  val format: String = "json",
)
