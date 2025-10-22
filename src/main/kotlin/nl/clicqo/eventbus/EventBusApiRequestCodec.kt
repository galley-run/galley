package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.openapi.validation.RequestParameter
import run.galley.cloud.crew.CrewRole

class EventBusApiRequestCodec : MessageCodec<EventBusApiRequest, EventBusApiRequest> {
  override fun encodeToWire(
    buffer: Buffer,
    s: EventBusApiRequest,
  ) {
    val pathParamsJson = JsonObject()
    s.pathParams?.forEach { (key, value) ->
      pathParamsJson.put(key, value.toString())
    }

    val queryJson = JsonObject()
    s.query?.forEach { (key, value) ->
      queryJson.put(key, value.toString())
    }

    val jsonObject =
      JsonObject()
        .put("pathParams", pathParamsJson)
        .put("body", s.body)
        .put("query", queryJson)
        .put("user", s.user?.principal())
        .put("version", s.version)
        .put("format", s.format)
        .put("crewRole", s.crewRole?.name)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusApiRequest {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    // Note: This is a simplified deserialization. You may need to properly
    // reconstruct RequestParameter objects based on your requirements.
    val pathParams = mutableMapOf<String, RequestParameter>()
    val query = mutableMapOf<String, RequestParameter>()

    return EventBusApiRequest(
      pathParams = pathParams,
      body = json.getJsonObject("body"),
      query = query,
      user = User.create(json.getJsonObject("user")),
      version = json.getString("version", "v1"),
      format = json.getString("format", "json"),
      crewRole = json.getString("crewRole")?.let(CrewRole::valueOf),
    )
  }

  override fun transform(s: EventBusApiRequest): EventBusApiRequest = s

  override fun name(): String = "EventBusApiRequestCodec"

  override fun systemCodecID(): Byte = -1
}
