package nl.clicqo.messaging.email

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class Recipients(
  private val value: List<String>,
) {
  companion object {
    @JvmStatic
    fun none(): Recipients = Recipients(emptyList())

    @JvmStatic
    fun one(recipient: String): Recipients = Recipients(listOf(recipient))

    @JvmStatic
    fun many(vararg recipient: String): Recipients = Recipients(recipient.toList())

    @JvmStatic
    fun many(recipients: List<String>): Recipients = Recipients(recipients)

    @JvmStatic
    fun from(json: JsonArray): Recipients = Recipients(json.list.map { it.toString() })
  }

  fun isEmpty(): Boolean = value.isEmpty()

  fun hasSingle(): Boolean = value.size == 1

  fun size(): Int = value.size

  fun add(recipient: String): Recipients = Recipients(value.plus(recipient))

  fun addAll(recipients: List<String>): Recipients = Recipients(value.plus(recipients))

  fun toList(): List<String> = value
}
