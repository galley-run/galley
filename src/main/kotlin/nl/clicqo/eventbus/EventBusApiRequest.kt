package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.openapi.validation.RequestParameter
import run.galley.cloud.crew.CrewRole

class EventBusApiRequest(
  // Headers
  val format: String = "v1",
  val version: String = "v1",
  val context: EventBusApiRequestContext = EventBusApiRequestContext(),
  val crewRole: CrewRole? = null,
  // Request Payload
  val pathParams: MutableMap<String, RequestParameter>? = null,
  val body: JsonObject? = null,
  val query: MutableMap<String, RequestParameter>? = null,
  val user: User? = null,
  // TODO: Add filter support for searching
  //  val filter: MutableMap<String, RequestParameter>,
  // TODO: Add pagination and sorting support
  //  val pagination: MutableMap<String, RequestParameter>,
  //  val sorting: MutableMap<String, RequestParameter>,
)
