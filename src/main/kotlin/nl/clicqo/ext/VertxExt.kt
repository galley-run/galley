package nl.kleilokaal.queue.modules

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.router.OpenAPIRoute
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusReplyException
import kotlin.coroutines.CoroutineContext

fun <T> EventBus.coroutineConsumer(
  context: CoroutineContext,
  address: String,
  fn: suspend (Message<T>) -> Unit,
): MessageConsumer<T?>? =
  consumer<T>(address) { message ->
    coroutineConsumerHandler(context, fn, message)
  }

fun <T> EventBus.coroutineLocalConsumer(
  context: CoroutineContext,
  address: String,
  fn: suspend (Message<T>) -> Unit,
): MessageConsumer<T?>? =
  localConsumer<T>(address) { message ->
    coroutineConsumerHandler(context, fn, message)
  }

private fun <T> coroutineConsumerHandler(
  context: CoroutineContext,
  fn: suspend (Message<T>) -> Unit,
  message: Message<T>,
) {
  CoroutineScope(context).launch {
    try {
      fn(message)
    } catch (e: ApiStatus) {
      message.reply(ApiStatusReplyException(e))
    } catch (e: ApiStatusReplyException) {
      message.reply(e)
    } catch (e: PgException) {
      message.reply(
        when {
          e.constraint.contains("uq_") -> ApiStatusReplyException(ApiStatus.PG_DUPLICATE_ENTRY)
          else -> ApiStatusReplyException(e)
        },
      )
    } catch (e: Exception) {
      message.reply(ApiStatusReplyException(e))
    }
  }
}

fun OpenAPIRoute.addCoroutineHandler(
  vertx: Vertx,
  block: suspend (rc: RoutingContext) -> Unit,
): OpenAPIRoute {
  val scope = CoroutineScope(vertx.dispatcher() + SupervisorJob())
  return this.addHandler { rc -> scope.launch { block(rc) } }
}
