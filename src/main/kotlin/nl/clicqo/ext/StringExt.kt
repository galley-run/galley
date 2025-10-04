package nl.clicqo.ext

import java.time.LocalDate
import java.util.UUID
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusException

fun String.camelCaseToSnakeCase(): String {
  return mapIndexed { index, c ->
    when {
      index < 1 -> c.lowercase()
      c.isUpperCase() -> "_${c.lowercase()}"
      else -> c.lowercase()
    }
  }.joinToString("")
}

fun String.toUUID(): UUID {
  return this.toUUID(ApiStatus.UUID_PARSE_EXCEPTION)
}

fun String.toUUID(apiStatusToThrow: ApiStatus): UUID {
  return try {
    UUID.fromString(this)
  } catch (_: IllegalArgumentException) {
    throw ApiStatusException(apiStatusToThrow)
  }
}

fun String.toLocalDate(): LocalDate {
  return try {
    LocalDate.parse(this)
  } catch (e: Exception) {
    throw ApiStatusException(ApiStatus.DATE_PARSE_EXCEPTION)
  }
}

fun String.isValidEmail(): Boolean {
  return this.matches(Regex("^[\\w\\-.+]+@([\\w-]+\\.)+[\\w-]{2,}\$"))
}
