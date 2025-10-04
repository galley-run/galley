package nl.clicqo.api

class ApiStatusException(val apiStatus: ApiStatus, val customMessage: String = "") : Exception() {
  override val message: String = ""

  override fun toString(): String {
    return customMessage.ifEmpty {
      apiStatus.message
    }
  }
}
