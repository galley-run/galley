package nl.clicqo.eventbus

import org.jooq.Field
import java.util.UUID

class EventBusQueryFilterBuilder {
  private val map = mutableMapOf<String, List<String>>()

  infix fun Field<*>.eq(value: String) {
    map[this.name] = listOf(value)
  }

  infix fun Field<*>.eq(value: UUID) {
    map[this.name] = listOf(value.toString())
  }

  infix fun Field<*>.isIn(values: Iterable<String>) {
    map[this.name] = values.toList()
  }

  infix fun Field<*>.isIn(values: Iterable<UUID>) {
    map[this.name] = values.map(UUID::toString)
  }

  fun build(): Map<String, List<String>> = map
}

fun filters(block: EventBusQueryFilterBuilder.() -> Unit): Map<String, List<String>> = EventBusQueryFilterBuilder().apply(block).build()
