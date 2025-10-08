package nl.clicqo.api

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure

class ApiStatusReplyException(
  val apiStatus: ApiStatus,
  override val message: String = "",
) : ReplyException(
    ReplyFailure.RECIPIENT_FAILURE,
    apiStatus.code,
    message.ifEmpty { apiStatus.message },
  ) {
  constructor(e: Exception) : this(ApiStatus.THROWABLE_EXCEPTION, e.message ?: "Exception: ${e.javaClass.name}")

  override fun toString(): String =
    message.ifEmpty {
      apiStatus.message
    }
}
