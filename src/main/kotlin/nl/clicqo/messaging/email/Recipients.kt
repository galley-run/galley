package nl.clicqo.messaging.email

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
  }

  fun isEmpty(): Boolean = value.isEmpty()

  fun hasSingle(): Boolean = value.size == 1

  fun size(): Int = value.size

  fun toList(): List<String> = value
}
