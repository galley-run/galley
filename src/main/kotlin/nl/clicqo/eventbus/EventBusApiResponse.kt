package nl.clicqo.eventbus

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import nl.clicqo.web.HttpStatus

data class EventBusApiResponse(
  // Headers
  val version: String = "v1",
  val format: String = "json",
  val httpStatus: HttpStatus? = null,
  // For the JSON:API Body spec
  val data: Any,
  val errors: JsonArray? = null,
  val meta: JsonObject? = null,
  val links: JsonObject? = null,
  val included: JsonArray? = null,
)
