package nl.clicqo.ext

import io.vertx.core.json.JsonObject
import java.util.UUID

fun JsonObject.getUUID(
  key: String,
  defaultValue: UUID?,
): UUID? =
  try {
    UUID.fromString(this.getString(key, defaultValue.toString()))
  } catch (e: IllegalArgumentException) {
    null
  } catch (e: NullPointerException) {
    null
  }

fun JsonObject.getUUID(key: String): UUID? = this.getUUID(key, null)

inline fun <T> T.applyIf(
  condition: Boolean,
  block: T.() -> T,
): T {
  return if (condition) this.block() else this
}
