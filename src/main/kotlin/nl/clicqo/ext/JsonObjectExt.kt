package nl.clicqo.ext

import io.vertx.core.json.JsonObject

fun JsonObject.keysToSnakeCase(): JsonObject = this.associate { (k, v) -> k.camelCaseToSnakeCase() to v }.run(JsonObject::mapFrom)
