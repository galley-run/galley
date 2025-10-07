package nl.clicqo.data

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import run.galley.cloud.model.BaseModel

data class DataPayload<T: BaseModel>(
  val item: T? = null,
  val items: List<T>? = null,
) {
  fun toSingle(): T? = item

  fun toMany(): List<T>? = items

  fun toCodec(): JsonObject {
    val payload = JsonObject()
    if (item != null) {
      payload
        .put("item", item.toJsonAPIResourceObject())
    } else {
      payload
        .put("items", JsonArray(items?.map { it.toJsonAPIResourceObject() }))
    }

    return payload
  }

  fun toJsonObject(): JsonObject {
    val payload = JsonObject()
    if (item != null) {
      payload
        .put("data", item.toJsonAPIResourceObject())
    } else {
      payload
        .put("data", JsonArray(items?.map { it.toJsonAPIResourceObject() }))
    }

    return payload
  }

  companion object {
    fun <T: BaseModel> one(item: T): DataPayload<T> = DataPayload(item = item)

    fun <T: BaseModel> many(items: List<T>?): DataPayload<T> = DataPayload(items = items ?: emptyList())
    fun <T: BaseModel> many(vararg items: T): DataPayload<T> = DataPayload(items = items.asList())

    inline fun <reified T : BaseModel> from(payload: JsonObject): DataPayload<T> {
      val clazz = T::class.java
      val modelClass = clazz.getDeclaredConstructor().newInstance()

      val item = payload.getJsonObject("item")?.let { modelClass.fromJsonAPIResourceObject<T>(it) }
      val items = payload.getJsonArray("items")?.map { modelClass.fromJsonAPIResourceObject<T>(it as JsonObject) }

      return DataPayload(item = item, items = items)
    }
  }
}
