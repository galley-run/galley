package nl.clicqo.api

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.eventbus.ReplyFailure

open class ApiStatusReplyException(
  open val apiStatus: ApiStatus,
  override val message: String = "",
  val sourcePointer: String? = null,
) : ReplyException(
    ReplyFailure.RECIPIENT_FAILURE,
    apiStatus.code,
    message.takeIf(String::isNotBlank) ?: apiStatus.message,
  ) {
  override val cause: Throwable?
    get() = apiStatus

  constructor(e: Exception) : this(ApiStatus.THROWABLE_EXCEPTION, e.message ?: "Exception: ${e.javaClass.name}")

  override fun toString(): String =
    message.ifEmpty {
      apiStatus.message
    }
}
