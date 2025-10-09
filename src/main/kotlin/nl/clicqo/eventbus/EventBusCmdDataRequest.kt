package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import run.galley.cloud.model.BaseModel
import java.util.UUID

/**
 * Represents a command data request for EventBus operations, typically used to manage or modify
 * resources in the system. Supports specifying identifiers, filters, user context, and data payloads.
 *
 * @param T The type of the model extending [BaseModel] which this request works with.
 * @property identifier Optional unique identifier for updating existing resources.
 * @property filters A map of filtering criteria, useful for targeting specific resources.
 * @property userId The ID of the user initiating the request, providing context for auditing or attribution.
 * @property payload Encapsulates the data payload, which can include a single or multiple items of type [T].
 */
data class EventBusCmdDataRequest(
  val identifier: UUID? = null, // Used for to update an existing resource.
  val filters: Map<String, List<String>> = emptyMap(), // Used for updating specific resources
  val userId: UUID? = null, // Used for creator context
  val payload: JsonObject? = null,
)
