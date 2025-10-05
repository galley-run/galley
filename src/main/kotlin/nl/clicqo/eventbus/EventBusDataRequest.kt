package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import io.vertx.openapi.validation.RequestParameter

class EventBusDataRequest(
  val identifiers: MutableMap<String, RequestParameter>? = null,
  val body: JsonObject? = null,
  val query: MutableMap<String, RequestParameter>? = null,
  // TODO: Add filter support for searching
  //  val filter: MutableMap<String, RequestParameter>,
  // TODO: Add pagination and sorting support
  //  val pagination: MutableMap<String, RequestParameter>,
  //  val sorting: MutableMap<String, RequestParameter>,
  val version: String = "v1",
)
