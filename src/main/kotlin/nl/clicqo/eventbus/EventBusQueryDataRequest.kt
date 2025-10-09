package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject
import nl.clicqo.api.Pagination
import nl.clicqo.api.SortField

/**
 * Request object for EventBus communication between Controller and Data Verticles.
 *
 * Use cases:
 * - GET operations: Use identifiers to specify which resource to fetch
 * - LIST operations: Use filters, sort, and pagination for querying collections
 *
 * Example GET:
 *   EventBusDataRequest(identifiers = mapOf("vesselId" to "123"))
 *
 * Example LIST:
 *   EventBusDataRequest(
 *     filters = mapOf("status" to listOf("active"), "type" to listOf("sailboat", "yacht")),
 *     sort = listOf(SortField("name", SortDirection.ASC)),
 *     pagination = Pagination(offset = 0, limit = 50)
 *   )
 */
data class EventBusQueryDataRequest(
  val identifiers: Map<String, String> = emptyMap(),
  val filters: Map<String, List<String>> = emptyMap(),
  val sort: List<SortField> = emptyList(),
  val pagination: Pagination? = null,
  val user: JsonObject? = null,
)
