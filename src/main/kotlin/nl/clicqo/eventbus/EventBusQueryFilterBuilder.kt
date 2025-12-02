package nl.clicqo.eventbus

import org.jooq.Field

class EventBusQueryFilterBuilder {
  private val map = mutableMapOf<String, List<String>>()

  infix fun <T> Field<T>.eq(value: T) {
    map[this.name] = listOf(value.toString())
  }

  infix fun <T> Field<T>.isIn(values: Iterable<T>) {
    map[this.name] = values.map { it.toString() }
  }

  fun <T> Field<T>.isIn(vararg values: T) {
    map[this.name] = values.map { it.toString() }
  }

  fun <T> Field<T>.`in`(values: Iterable<T>) = isIn(values)

  fun build(): Map<String, List<String>> = map
}

fun filters(block: EventBusQueryFilterBuilder.() -> Unit): Map<String, List<String>> = EventBusQueryFilterBuilder().apply(block).build()
