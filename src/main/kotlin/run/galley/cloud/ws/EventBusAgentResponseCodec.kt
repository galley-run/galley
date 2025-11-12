package run.galley.cloud.ws

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import nl.clicqo.ext.getUUID

class EventBusAgentResponseCodec : MessageCodec<EventBusAgentResponse, EventBusAgentResponse> {
  override fun encodeToWire(
    buffer: Buffer,
    s: EventBusAgentResponse,
  ) {
    val json =
      JsonObject()
        .put("vesselEngineId", s.vesselEngineId.toString())
        .put("payload", s.payload)

    val encoded = json.encode()
    val length = encoded.toByteArray(Charsets.UTF_8).size

    buffer.appendInt(length)
    buffer.appendString(encoded)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusAgentResponse {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonStr = buffer.getString(position, position + length)
    val json = JsonObject(jsonStr)

    val vesselEngineId = json.getUUID("vesselEngineId") ?: throw Exception("VesselEngine ID not found")
    val payload = json.getJsonObject("payload")

    return EventBusAgentResponse(
      vesselEngineId = vesselEngineId,
      payload = payload,
    )
  }

  override fun transform(s: EventBusAgentResponse): EventBusAgentResponse {
    // Object is immutable; safe om door te geven
    return s
  }

  override fun name(): String = "EventBusAgentResponseCodec"

  override fun systemCodecID(): Byte = -1
}
