package nl.kleilokaal.queue.modules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields

@Deprecated("Use LocalDate.toStartOfWeek() instead")
fun LocalDate.toWeek(): Long {
  val weekOfYear = this.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toLong()
  val weekBasedYear = this.get(IsoFields.WEEK_BASED_YEAR).toLong()
  return "$weekBasedYear${weekOfYear.toString().padStart(2, '0')}".toLong()
}

fun LocalDate.generateWeekRange(weeksInAdvance: Long): List<LocalDate> {
  return (0..weeksInAdvance).map { weekOffset ->
    this.plusWeeks(weekOffset).toStartOfWeek()
  }
}

fun LocalDate.toStartOfWeek(): LocalDate {
  return this.with(DayOfWeek.MONDAY)
}

fun LocalDate.toEndOfWeek(): LocalDate {
  return this.toStartOfWeek().plusDays(6)
}
