package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import io.vertx.openapi.validation.RequestParameter

class EventBusDataRequestCodec : MessageCodec<EventBusDataRequest, EventBusDataRequest> {

  override fun encodeToWire(buffer: Buffer, s: EventBusDataRequest) {
    val identifiersJson = JsonObject()
    s.identifiers?.forEach { (key, value) ->
      identifiersJson.put(key, value.toString())
    }

    val queryJson = JsonObject()
    s.query?.forEach { (key, value) ->
      queryJson.put(key, value.toString())
    }

    val jsonObject = JsonObject()
      .put("identifiers", identifiersJson)
      .put("body", s.body)
      .put("query", queryJson)
      .put("version", s.version)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  override fun decodeFromWire(pos: Int, buffer: Buffer): EventBusDataRequest {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    // Note: This is a simplified deserialization. You may need to properly
    // reconstruct RequestParameter objects based on your requirements.
    val identifiers = mutableMapOf<String, RequestParameter>()
    val query = mutableMapOf<String, RequestParameter>()

    return EventBusDataRequest(
      identifiers = identifiers,
      body = json.getJsonObject("body"),
      query = query,
      version = json.getString("version", "v1")
    )
  }

  override fun transform(s: EventBusDataRequest): EventBusDataRequest = s

  override fun name(): String = "EventBusDataRequestCodec"

  override fun systemCodecID(): Byte = -1
}
