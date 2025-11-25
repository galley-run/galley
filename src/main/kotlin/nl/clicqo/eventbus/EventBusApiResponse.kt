package nl.clicqo.eventbus

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.ClusterSerializable
import nl.clicqo.web.HttpStatus

data class EventBusApiResponse(
  val data: ClusterSerializable? = null,
  // Headers
  val version: String = "v1",
  val format: String = "json",
  val contentType: String = "application/vnd.galley.$version+$format",
  val httpStatus: HttpStatus = HttpStatus.NoContent,
  // For the JSON:API Body spec
  val errors: JsonArray? = null,
  val meta: JsonObject? = null,
  val links: JsonObject? = null,
  val included: JsonArray? = null,
)
