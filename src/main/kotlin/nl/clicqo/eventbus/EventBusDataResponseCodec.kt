package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload

class EventBusDataResponseCodec : MessageCodec<EventBusDataResponse, EventBusDataResponse> {

  override fun encodeToWire(buffer: Buffer, response: EventBusDataResponse) {
    val jsonObject = JsonObject()
      .put("payload", response.payload)
      .put("metadata", response.metadata)

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

    val payload = json.getJsonObject("payload", JsonObject())
    val metadata = json.getJsonObject("metadata")

    return EventBusDataResponse(
      payload = DataPayload.from(payload),
      metadata = metadata
    )
  }

  override fun transform(response: EventBusDataResponse): EventBusDataResponse = response

  override fun name(): String = "EventBusDataResponseCodec"

  override fun systemCodecID(): Byte = -1
}
