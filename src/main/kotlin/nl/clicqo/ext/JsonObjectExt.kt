package nl.clicqo.ext

import io.vertx.core.json.JsonObject
import org.jooq.Record
import org.jooq.TableField

fun JsonObject.keysToSnakeCase(): JsonObject = this.associate { (k, v) -> k.camelCaseToSnakeCase() to v }.run(JsonObject::mapFrom)

inline fun <T> JsonObject.applyIfPresent(
  key: String,
  getter: JsonObject.(String) -> T,
  setter: (T) -> Unit,
) {
  if (containsKey(key)) {
    setter(this.getter(key))
  }
}

inline fun <T, S, R : Record> JsonObject.applyIfPresent(
  key: TableField<R, T>,
  getter: JsonObject.(String) -> S,
  setter: (S) -> Unit,
) {
  this.applyIfPresent(key.name, getter, setter)
}
