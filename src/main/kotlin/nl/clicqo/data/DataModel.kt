package nl.clicqo.data

import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row

abstract class DataModel {

  abstract fun toJsonObject(): JsonObject

  fun toJsonAPIResourceObject(): JsonObject {
    val attributes = toJsonObject()
    val id = attributes.getString("id")
    attributes.remove("id")
    return JsonObject()
      .put("type", this::class.simpleName?.lowercase())
      .put("id", id)
      .put("attributes", attributes)
  }

  interface DataModelFactory<T : DataModel> {
    fun from(data: JsonObject): T
    fun from(row: Row): T
  }
}
