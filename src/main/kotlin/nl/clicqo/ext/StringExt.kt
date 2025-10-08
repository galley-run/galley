package nl.clicqo.ext

import nl.clicqo.api.ApiStatus
import java.time.LocalDate
import java.util.UUID

fun String.camelCaseToSnakeCase(): String =
  mapIndexed { index, c ->
    when {
      index < 1 -> c.lowercase()
      c.isUpperCase() -> "_${c.lowercase()}"
      else -> c.lowercase()
    }
  }.joinToString("")

fun String.toUUID(): UUID = this.toUUID(ApiStatus.UUID_PARSE_EXCEPTION)

fun String.toUUID(apiStatusToThrow: ApiStatus): UUID =
  try {
    UUID.fromString(this)
  } catch (_: IllegalArgumentException) {
    throw apiStatusToThrow
  }

fun String.toLocalDate(): LocalDate =
  try {
    LocalDate.parse(this)
  } catch (e: Exception) {
    throw ApiStatus.DATE_PARSE_EXCEPTION
  }

fun String.isValidEmail(): Boolean = this.matches(Regex("^[\\w\\-.+]+@([\\w-]+\\.)+[\\w-]{2,}\$"))
