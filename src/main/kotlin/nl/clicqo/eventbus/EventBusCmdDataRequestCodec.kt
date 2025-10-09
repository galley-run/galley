package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import nl.clicqo.ext.getUUID
import run.galley.cloud.model.BaseModel

class EventBusCmdDataRequestCodec : MessageCodec<EventBusCmdDataRequest, EventBusCmdDataRequest> {
  override fun encodeToWire(
    buffer: Buffer,
    request: EventBusCmdDataRequest,
  ) {
    val filtersJson = JsonObject()
    request.filters.forEach { (key, values) ->
      filtersJson.put(key, JsonArray(values))
    }

    val jsonObject =
      JsonObject()
        .put("identifier", request.identifier)
        .put("filters", filtersJson)
        .put("userId", request.userId)
        .put("payload", request.payload)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  @Suppress("UNCHECKED_CAST")
  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusCmdDataRequest {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    val identifier = json.getUUID("identifier")

    val filters = mutableMapOf<String, List<String>>()
    json.getJsonObject("filters", JsonObject()).forEach { (key, value) ->
      if (value is JsonArray) {
        filters[key] = value.map { it.toString() }
      }
    }

    val userId = json.getUUID("userId")
    val payload = json.getJsonObject("payload", JsonObject())

    return EventBusCmdDataRequest(
      identifier = identifier,
      filters = filters,
      userId = userId,
      payload = payload,
    )
  }

  override fun transform(request: EventBusCmdDataRequest): EventBusCmdDataRequest = request

  override fun name(): String = "EventBusCmdDataRequestCodec"

  override fun systemCodecID(): Byte = -1
}
