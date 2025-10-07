package nl.clicqo.data

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import run.galley.cloud.model.BaseModel

data class DataPayload(
  val items: List<BaseModel>
) {
  fun isSingle(): Boolean = items.size == 1

  fun isMany(): Boolean = items.size > 1

  @Suppress("UNCHECKED_CAST")
  fun <T> toSingle(): T? = items.first() as T?

  @Suppress("UNCHECKED_CAST")
  fun <T> toMany(): List<T> = items as List<T>

  fun toJsonObject(): JsonObject {
    val payload = JsonObject()
    if (isSingle()) {
      payload
        .put("data", items.first().toJsonAPIResourceObject())
    } else {
      payload
        .put("data", JsonArray(items.map { it.toJsonAPIResourceObject() }))
    }

    return payload
  }

  fun toCodec(): JsonObject {
    val payload = JsonObject()
      .put("type", items.first()::class.simpleName)

    if (isSingle()) {
      payload
        .put("data", items.first().toJsonAPIResourceObject())
    } else {
      payload
        .put("data", JsonArray(items.map { it.toJsonAPIResourceObject() }))
    }

    return payload
  }

  companion object {
    fun one(item: BaseModel): DataPayload = DataPayload(listOf(item))

    fun many(items: List<BaseModel>?): DataPayload = DataPayload(items ?: emptyList())
    fun many(vararg items: BaseModel): DataPayload = DataPayload(items.asList())

    fun from(payload: JsonObject): DataPayload {
      val clazz = Class.forName(payload.getString("type"))
      val modelClass = (clazz.getDeclaredConstructor().newInstance() as BaseModel)
      val data = payload.getValue("data")
      val items = when (data) {
        is JsonObject -> listOf(modelClass.fromJsonAPIResourceObject(data))
        is JsonArray -> data.map {
          modelClass.fromJsonAPIResourceObject(it as JsonObject)
        }

        else -> emptyList()
      }
      return DataPayload(items)
    }
  }
}
