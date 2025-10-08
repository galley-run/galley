package run.galley.cloud.model

import io.vertx.core.json.JsonObject

interface BaseModel {
  fun toJsonAPIResourceObject(): JsonObject {
    val attributes =
      JsonObject()
        .put(
          "id",
          this::class.java
            .getDeclaredFields()
            .firstOrNull { it.name == "id" }
            ?.let {
              it.isAccessible = true
              it.get(this)
            },
        ).apply {
          this@BaseModel::class.java
            .getDeclaredFields()
            .filter { it.name != "id" }
            .forEach { field ->
              field.isAccessible = true
              val value = field.get(this@BaseModel) ?: return@forEach
              put(field.name, value)
            }
        }
    val id = attributes.getString("id")
    attributes.remove("id")
    return JsonObject()
      .put("type", this::class.simpleName?.lowercase())
      .put("id", id)
      .put("attributes", attributes)
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : BaseModel> fromJsonAPIResourceObject(json: JsonObject): T {
    val id = json.getString("id")
    val attributes = json.getJsonObject("attributes")

    this::class.java
      .getDeclaredFields()
      .forEach { field ->
        field.isAccessible = true
        when (field.name) {
          "id" -> field.set(this, id)
          else -> {
            val value = attributes.getValue(field.name)
            if (value != null) {
              field.set(this, value)
            }
          }
        }
      }

    return this as T
  }
}
