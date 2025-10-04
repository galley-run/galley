package nl.kleilokaal.queue.modules

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.router.OpenAPIRoute
import io.vertx.kotlin.coroutines.dispatcher
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusException
import nl.clicqo.api.ApiStatusReplyException

//fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit): Route {
//  handler { ctx ->
//    GlobalScope.launch(ctx.vertx().dispatcher()) {
//      try {
//        fn(ctx)
//      } catch (e: Exception) {
//        ctx.fail(e)
//      }
//    }
//  }
//  return this
//}
//
//fun Route.coroutineHandler(
//  permission: String,
//  fn: suspend (RoutingContext) -> Unit,
//): Route {
//  handler { ctx ->
//    GlobalScope.launch(ctx.vertx().dispatcher()) {
//      try {
//        if (permission.isNotBlank() && ctx.user() == null) {
//          ctx.response().putHeader(io.vertx.core.http.HttpHeaders.REFERER, ctx.request().absoluteURI())
//          ctx.put("referer", ctx.request().absoluteURI())
//          ctx.reroute(HttpMethod.GET, "/account/login")
//          return@launch
//        }
//
//        if (ctx.user() == null || ctx.user().hasAmr(permission)) {
//          fn(ctx)
//        } else {
//          throw ApiStatusException(ApiStatus.FAILED_AUTHENTICATION)
//        }
//      } catch (e: Exception) {
//        ctx.fail(e)
//      }
//    }
//  }
//  return this
//}
//
//fun Route.coroutineHandler(
//  permission: String,
//  fn: suspend (RoutingContext) -> Unit,
//  elseFn: suspend (RoutingContext) -> Unit,
//): Route {
//  handler { ctx ->
//    GlobalScope.launch(ctx.vertx().dispatcher()) {
//      try {
//        if (permission.isNotBlank() && ctx.user() == null) {
//          ctx.response().putHeader(io.vertx.core.http.HttpHeaders.REFERER, ctx.request().absoluteURI())
//          ctx.put("referer", ctx.request().absoluteURI())
//          ctx.reroute(HttpMethod.GET, "/account/login")
//          return@launch
//        }
//
//        if (ctx.user() == null || ctx.user().hasAmr(permission)) {
//          fn(ctx)
//        } else {
//          elseFn(ctx)
//        }
//      } catch (e: Exception) {
//        ctx.fail(e)
//      }
//    }
//  }
//  return this
//}
//
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
): MessageConsumer<T?>? = localConsumer<T>(address) { message ->
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
    } catch (e: ApiStatusReplyException) {
      message.reply(e)
    } catch (e: ApiStatusException) {
      message.reply(ApiStatusReplyException(e))
    } catch (e: Exception) {
      message.reply(ApiStatusReplyException(e))
    }
  }
}

fun OpenAPIRoute.addCoroutineHandler(vertx: Vertx, block: suspend (rc: RoutingContext) -> Unit): OpenAPIRoute {
  val scope = CoroutineScope(vertx.dispatcher() + SupervisorJob())
  return this.addHandler { rc -> scope.launch { block(rc) } }
}
