package nl.clicqo.data

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class DataPayload(
  val items: List<DataModel>
) {
  fun isSingle(): Boolean = items.size == 1

  fun isMany(): Boolean = items.size > 1

  fun toJsonObject(): JsonObject {
    return if (isSingle()) {
      JsonObject().put("data", items.first().toJsonAPIResourceObject())
    } else {
      JsonObject().put("data", JsonArray(items.map { it.toJsonAPIResourceObject() }))
    }
  }

  companion object {
    fun one(item: DataModel): JsonObject = DataPayload(listOf(item)).toJsonObject()

    fun many(vararg items: DataModel): JsonObject = DataPayload(items.asList()).toJsonObject()
  }
}
