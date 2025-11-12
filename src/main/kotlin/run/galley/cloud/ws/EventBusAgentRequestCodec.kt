package run.galley.cloud.ws

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import nl.clicqo.ext.getUUID

class EventBusAgentRequestCodec : MessageCodec<EventBusAgentRequest, EventBusAgentRequest> {
  override fun encodeToWire(
    buffer: Buffer,
    s: EventBusAgentRequest,
  ) {
    val json =
      JsonObject()
        .put("vesselEngineId", s.vesselEngineId.toString())
        .put("action", s.action)
        .put("payload", s.payload)
        .put("replyTo", s.replyTo)

    val encoded = json.encode()
    val length = encoded.toByteArray(Charsets.UTF_8).size

    buffer.appendInt(length)
    buffer.appendString(encoded)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusAgentRequest {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonStr = buffer.getString(position, position + length)
    val json = JsonObject(jsonStr)

    val vesselEngineId = json.getUUID("vesselEngineId") ?: throw Exception("VesselEngine ID not found")
    val action = json.getString("action")
    val payload = json.getJsonObject("payload")
    val replyTo = json.getString("replyTo")

    return EventBusAgentRequest(
      vesselEngineId = vesselEngineId,
      action = action,
      payload = payload,
      replyTo = replyTo,
    )
  }

  override fun transform(s: EventBusAgentRequest): EventBusAgentRequest {
    // Object is immutable; safe om door te geven
    return s
  }

  override fun name(): String = "EventBusAgentRequestCodec"

  override fun systemCodecID(): Byte = -1
}
