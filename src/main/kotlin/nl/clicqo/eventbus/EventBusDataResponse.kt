package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject

/**
 * Response object for EventBus communication from Data Verticles to Controller Verticles.
 *
 * @property payload The actual data payload (typically JSON:API formatted)
 * @property metadata Optional metadata for pagination info, totals, etc.
 *
 * Example metadata for pagination:
 *   JsonObject()
 *     .put("total", 150)
 *     .put("offset", 0)
 *     .put("limit", 50)
 *     .put("hasMore", true)
 */
data class EventBusDataResponse(
  val payload: JsonObject,
  val metadata: JsonObject? = null
)
