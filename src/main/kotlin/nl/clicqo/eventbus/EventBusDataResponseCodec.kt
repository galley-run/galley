package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

class EventBusDataResponseCodec : MessageCodec<EventBusDataResponse, EventBusDataResponse> {
  override fun encodeToWire(buffer: Buffer, s: EventBusDataResponse?) {
    val jsonObject = JsonObject()
      .put("payload", s?.payload)
      .put("version", s?.version)
      .put("format", s?.format)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  override fun decodeFromWire(pos: Int, buffer: Buffer): EventBusDataResponse {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    return EventBusDataResponse(
      payload = json.getJsonObject("payload"),
      version = json.getString("version", "v1"),
      format = json.getString("format", "json")
    )
  }

  override fun transform(s: EventBusDataResponse): EventBusDataResponse = s

  override fun name(): String = "EventBusDataResponseCodec"

  override fun systemCodecID(): Byte = -1
}
