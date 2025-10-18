package nl.clicqo.ext

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.eventbus.ReplyFailure.RECIPIENT_FAILURE
import io.vertx.core.internal.ContextInternal
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.router.OpenAPIRoute
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.pgclient.PgException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusReplyException
import org.postgresql.util.PSQLState
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun OpenAPIRoute.addCoroutineHandler(
  vertx: Vertx,
  block: suspend (rc: RoutingContext) -> Unit,
): OpenAPIRoute {
  val scope = CoroutineScope(vertx.dispatcher() + SupervisorJob())
  return this.addHandler { rc -> scope.launch { block(rc) } }
}

fun CoroutineScope.coroutineEventBus(block: CoroutineEventBusSupport.() -> Unit) {
  val receiver =
    object : CoroutineEventBusSupport {
      override val coroutineContext = this@coroutineEventBus.coroutineContext
    }
  with(receiver) {
    block()
  }
}

interface CoroutineEventBusSupport : CoroutineScope {
  /**
   }
   * Similar to [EventBus.consumer] but using a suspending [handler].
   *
   * The coroutine context is inherited from the [CoroutineScope].
   * Additional context elements can be specified with the [context] argument.
   *
   * @param context additional context elements, [EmptyCoroutineContext] by default
   */
  fun <T> EventBus.coConsumer(
    address: String,
    context: CoroutineContext = EmptyCoroutineContext,
    handler: suspend (Message<T>) -> Unit,
  ): MessageConsumer<T> = consumer<T>(address).coHandler(context, handler)

  /**
   * Similar to [MessageConsumer.handler] but using a suspending [handler].
   *
   * The coroutine context is inherited from the [CoroutineScope].
   * Additional context elements can be specified with the [context] argument.
   *
   * @param context additional context elements, [EmptyCoroutineContext] by default
   */
  fun <T> MessageConsumer<T>.coHandler(
    context: CoroutineContext = EmptyCoroutineContext,
    handler: suspend (Message<T>) -> Unit,
  ): MessageConsumer<T> =
    handler {
      launch((ContextInternal.current()?.dispatcher() ?: EmptyCoroutineContext) + context) {
        try {
          handler(it)
        } catch (e: PgException) {
          it.reply(
            when (e.sqlState) {
              PSQLState.UNIQUE_VIOLATION.state -> ApiStatusReplyException(ApiStatus.PG_FAILED_CONSTRAINT_DUPLICATE)
              else -> ApiStatusReplyException(ApiStatus.FAILED)
            },
          )
        } catch (e: ApiStatus) {
          it.reply(ApiStatusReplyException(e))
        } catch (e: ApiStatusReplyException) {
          it.reply(e)
        } catch (e: Exception) {
          it.fail(RECIPIENT_FAILURE.toInt(), e.message)
        }
      }
    }
}
