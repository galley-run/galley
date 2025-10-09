package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload
import nl.clicqo.ext.getUUID
import run.galley.cloud.model.BaseModel

class EventBusCmdDataRequestCodec<T : BaseModel> : MessageCodec<EventBusCmdDataRequest<T>, EventBusCmdDataRequest<T>> {
  override fun encodeToWire(
    buffer: Buffer,
    request: EventBusCmdDataRequest<T>,
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
        .put("payload", request.payload?.toCodec())

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  @Suppress("UNCHECKED_CAST")
  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusCmdDataRequest<T> {
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

    // We need to use the type from the JSON payload since T is not reified here
    val type = payload.getString("type")
    val clazz = Class.forName("run.galley.cloud.model.${type.replaceFirstChar { it.uppercase() }}") as Class<T>

    return EventBusCmdDataRequest(
      identifier = identifier,
      filters = filters,
      userId = userId,
      payload = createDataPayload(payload, clazz),
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun createDataPayload(
    payload: JsonObject,
    clazz: Class<T>,
  ): DataPayload<T> {
    val modelClass = clazz.getDeclaredConstructor().newInstance()

    val item = payload.getJsonObject("item")?.let { modelClass.fromJsonAPIResourceObject<T>(it) }
    val items =
      payload.getJsonArray("items")?.map {
        clazz.getDeclaredConstructor().newInstance().fromJsonAPIResourceObject<T>(it as JsonObject)
      }

    return DataPayload(item = item, items = items)
  }

  override fun transform(request: EventBusCmdDataRequest<T>): EventBusCmdDataRequest<T> = request

  override fun name(): String = "EventBusCmdDataRequestCodec"

  override fun systemCodecID(): Byte = -1
}
