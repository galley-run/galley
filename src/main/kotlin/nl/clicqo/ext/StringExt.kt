package nl.clicqo.ext

import nl.clicqo.api.ApiStatus
import java.time.LocalDate
import java.util.UUID
import kotlin.io.encoding.Base64

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

fun String.isValidEmail(): Boolean = this.matches(Regex("^[\\w\\-.+]+@([\\w-]+\\.)+[\\w-]{2,}$"))

fun String.toSingular(): String =
  when (this) {
    "crew" -> "crew"
    else ->
      when {
        endsWith("ies") -> dropLast(3) + "y"
        endsWith("s") && !endsWith("ss") -> dropLast(1)
        else -> this
      }
  }

fun String.toBase64() = Base64.encode(this.encodeToByteArray())

fun ByteArray.toBase64() = Base64.encode(this)

fun String.fromBase64(): String {
  val raw = this.substringAfter(",", this).replace("\\s".toRegex(), "")
  val isUrlSafe = raw.indexOf('-') >= 0 || raw.indexOf('_') >= 0
  val padLen = (4 - (raw.length % 4)) % 4
  val padded = raw + "=".repeat(padLen)
  val bytes = if (isUrlSafe) Base64.UrlSafe.decode(padded) else Base64.decode(padded)
  return bytes.decodeToString() // UTF-8
}
