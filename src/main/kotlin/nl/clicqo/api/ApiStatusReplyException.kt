package nl.clicqo.api

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure

class ApiStatusReplyException(val apiStatus: ApiStatus, val customMessage: String = "") : ReplyException(
  ReplyFailure.RECIPIENT_FAILURE,
  apiStatus.code,
  customMessage.ifEmpty { apiStatus.message },
) {
  constructor(e: ApiStatusException) : this(e.apiStatus, e.customMessage) {
    initCause(e)
  }

  constructor(e: Throwable) : this(ApiStatus.THROWABLE_EXCEPTION, e.message ?: "Exception: ${e.javaClass.name}") {
    initCause(e)
  }

  override val message: String = ""

  override fun toString(): String {
    return customMessage.ifEmpty {
      apiStatus.message
    }
  }
}
