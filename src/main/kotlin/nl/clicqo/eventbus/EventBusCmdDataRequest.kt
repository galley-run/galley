package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import java.util.UUID

/**
 * Represents a command data request for EventBus operations, typically used to manage or modify
 * resources in the system. Supports specifying identifiers, filters, user context, and data payloads.
 *
 * @property payload Encapsulates the data payload as JsonObject
 * @property identifier Optional unique identifier for updating existing resources.
 * @property filters A map of filtering criteria, useful for targeting specific resources.
 * @property userId The ID of the user initiating the request, providing context for auditing or attribution.
 */
data class EventBusCmdDataRequest(
  val payload: JsonObject? = null,
  val identifier: UUID? = null, // Used for to update an existing resource.
  val filters: Map<String, List<String>> = emptyMap(), // Used for updating specific resources
  val userId: UUID? = null, // Used for creator context
)
