package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import nl.clicqo.web.HttpStatus

class EventBusApiResponseCodec : MessageCodec<EventBusApiResponse, EventBusApiResponse> {
  override fun encodeToWire(
    buffer: Buffer,
    s: EventBusApiResponse?,
  ) {
    val jsonObject =
      JsonObject()
        .put("data", s?.data)
        .put("meta", s?.meta)
        .put("links", s?.links)
        .put("included", s?.included)
        .put("errors", s?.errors)
        .put("version", s?.version)
        .put("format", s?.format)
        .put("httpStatus", s?.httpStatus)

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
      data = json.getJsonObject("data"),
      meta = json.getJsonObject("meta"),
      links = json.getJsonObject("links"),
      included = json.getJsonArray("included"),
      errors = json.getJsonArray("errors"),
      version = json.getString("version", "v1"),
      format = json.getString("format", "json"),
      httpStatus = json.getString("httpStatus")?.let { HttpStatus.valueOf(it) } ?: HttpStatus.NoContent,
    )
  }

  override fun transform(s: EventBusApiResponse): EventBusApiResponse = s

  override fun name(): String = "EventBusApiResponseCodec"

  override fun systemCodecID(): Byte = -1
}
