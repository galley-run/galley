package nl.clicqo.eventbus

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload
import run.galley.cloud.model.BaseModel

class EventBusDataResponseCodec<T : BaseModel> : MessageCodec<EventBusDataResponse<T>, EventBusDataResponse<T>> {
  override fun encodeToWire(
    buffer: Buffer,
    response: EventBusDataResponse<T>,
  ) {
    val jsonObject =
      JsonObject()
        .put("payload", response.payload?.toCodec())
        .put("metadata", response.metadata)

    val bytes = jsonObject.toBuffer()
    buffer.appendInt(bytes.length())
    buffer.appendBuffer(bytes)
  }

  @Suppress("UNCHECKED_CAST")
  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EventBusDataResponse<T> {
    var position = pos
    val length = buffer.getInt(position)
    position += 4

    val jsonBytes = buffer.getBuffer(position, position + length)
    val json = JsonObject(jsonBytes)

    val payload = json.getJsonObject("payload", JsonObject())
    val metadata = json.getJsonObject("metadata")

    // We need to use the type from the JSON payload since T is not reified here
    val type = payload.getString("type")
    val clazz = Class.forName("run.galley.cloud.model.${type.replaceFirstChar { it.uppercase() }}") as Class<T>

    return EventBusDataResponse(
      payload = createDataPayload(payload, clazz),
      metadata = metadata,
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

  override fun transform(response: EventBusDataResponse<T>): EventBusDataResponse<T> = response

  override fun name(): String = "EventBusDataResponseCodec"

  override fun systemCodecID(): Byte = -1
}
