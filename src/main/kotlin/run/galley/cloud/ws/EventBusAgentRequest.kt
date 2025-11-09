package run.galley.cloud.ws

import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatus
import java.util.UUID

data class EventBusAgentRequest(
  val vesselEngineId: UUID,
  var action: String? = null, // The eventbus address in the Galley Agent's Eventbus we dispatch to
  val payload: JsonObject,
  val replyTo: String,
) {
  fun toSocketMessage(): String {
    if (action.isNullOrBlank()) {
      throw ApiStatus.AGENT_SEND_ACTION_REQUIRED
    }

    return JsonObject()
      .put("vesselEngineId", vesselEngineId)
      .put("action", action)
      .put("payload", payload)
      .put("replyTo", replyTo)
      .encode()
  }
}
