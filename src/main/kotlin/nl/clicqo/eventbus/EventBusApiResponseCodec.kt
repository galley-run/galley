package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

class EventBusApiResponseCodec : MessageCodec<EventBusApiResponse, EventBusApiResponse> {
  override fun encodeToWire(
    buffer: Buffer,
    s: EventBusApiResponse?,
  ) {
    val jsonObject =
      JsonObject()
        .put("payload", s?.payload)
        .put("version", s?.version)
        .put("format", s?.format)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusApiResponse {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    return EventBusApiResponse(
      payload = json.getJsonObject("payload"),
      version = json.getString("version", "v1"),
      format = json.getString("format", "json"),
    )
  }

  override fun transform(s: EventBusApiResponse): EventBusApiResponse = s

  override fun name(): String = "EventBusApiResponseCodec"

  override fun systemCodecID(): Byte = -1
}
