package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import nl.clicqo.api.Pagination
import nl.clicqo.api.SortDirection
import nl.clicqo.api.SortField

class EventBusDataRequestCodec : MessageCodec<EventBusDataRequest, EventBusDataRequest> {
  override fun encodeToWire(
    buffer: Buffer,
    request: EventBusDataRequest,
  ) {
    val identifiersJson = JsonObject(request.identifiers)

    val filtersJson = JsonObject()
    request.filters.forEach { (key, values) ->
      filtersJson.put(key, JsonArray(values))
    }

    val sortJson =
      JsonArray(
        request.sort.map { sortField ->
          JsonObject()
            .put("field", sortField.field)
            .put("direction", sortField.direction.name)
        },
      )

    val paginationJson =
      request.pagination?.let {
        JsonObject()
          .put("offset", it.offset)
          .put("limit", it.limit)
      }

    val jsonObject =
      JsonObject()
        .put("identifiers", identifiersJson)
        .put("filters", filtersJson)
        .put("sort", sortJson)
        .put("pagination", paginationJson)
        .put("user", request.user)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusDataRequest {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    val identifiers =
      json
        .getJsonObject("identifiers", JsonObject())
        .map
        .mapValues { it.value.toString() }

    val filters = mutableMapOf<String, List<String>>()
    json.getJsonObject("filters", JsonObject()).forEach { (key, value) ->
      if (value is JsonArray) {
        filters[key] = value.map { it.toString() }
      }
    }

    val sort =
      json.getJsonArray("sort", JsonArray()).mapNotNull {
        if (it is JsonObject) {
          SortField(
            field = it.getString("field"),
            direction = SortDirection.valueOf(it.getString("direction")),
          )
        } else {
          null
        }
      }

    val pagination =
      json.getJsonObject("pagination")?.let {
        Pagination(
          offset = it.getInteger("offset", 0),
          limit = it.getInteger("limit", Pagination.DEFAULT_LIMIT),
        )
      }

    val user = json.getJsonObject("user")

    return EventBusDataRequest(
      identifiers = identifiers,
      filters = filters,
      sort = sort,
      pagination = pagination,
      user = user,
    )
  }

  override fun transform(request: EventBusDataRequest): EventBusDataRequest = request

  override fun name(): String = "EventBusDataRequestCodec"

  override fun systemCodecID(): Byte = -1
}
