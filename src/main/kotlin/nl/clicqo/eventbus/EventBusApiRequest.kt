package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.openapi.validation.RequestParameter

class EventBusApiRequest(
  val identifiers: MutableMap<String, RequestParameter>? = null,
  val body: JsonObject? = null,
  val query: MutableMap<String, RequestParameter>? = null,
  val user: User? = null,
  // TODO: Add filter support for searching
  //  val filter: MutableMap<String, RequestParameter>,
  // TODO: Add pagination and sorting support
  //  val pagination: MutableMap<String, RequestParameter>,
  //  val sorting: MutableMap<String, RequestParameter>,
  val version: String = "v1",
)
