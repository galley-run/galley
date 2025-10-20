package nl.clicqo.api

import io.vertx.core.json.JsonObject

class ApiErrorSource {
  private var pointer: String? = null

  fun setPointer(pointer: String): ApiErrorSource {
    this.pointer = pointer
    return this
  }

  fun getPointer(): String? = pointer

  fun toJsonObject(): JsonObject = JsonObject().put("pointer", pointer)
}
