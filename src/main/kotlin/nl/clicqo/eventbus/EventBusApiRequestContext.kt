package nl.clicqo.eventbus

import io.vertx.core.json.JsonObject

class EventBusApiRequestContext(
  val userAgent: String = "",
  val remoteIp: String = "",
) {
  companion object {
    fun fromJsonObject(jsonObject: JsonObject): EventBusApiRequestContext =
      EventBusApiRequestContext(
        userAgent = jsonObject.getString("userAgent"),
        remoteIp = jsonObject.getString("remoteIp"),
      )
  }

  fun toJsonObject(): JsonObject = JsonObject.mapFrom(this)
}
