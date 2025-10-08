package nl.clicqo.api

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.openapi.router.RouterBuilder
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.openapi.contract.OpenAPIContract
import run.galley.cloud.web.JWT

abstract class OpenAPIBridgeRouter(
  open val vertx: Vertx,
  open val config: JsonObject,
) {
  protected lateinit var openAPIRouterBuilder: RouterBuilder
  lateinit var authProvider: JWTAuth

  suspend fun initialize(): OpenAPIBridgeRouter {
    val openAPIFile = config.getJsonObject("api").getString("openapiFile", "openapi.yaml")
    val contract = OpenAPIContract.from(vertx, openAPIFile).coAwait()
    this.openAPIRouterBuilder = RouterBuilder.create(vertx, contract)
    this.authProvider = JWT.authProvider(vertx, config)

    return this
  }

  abstract suspend fun buildRouter(): RouterBuilder

  suspend fun catchAll(
    routingContext: RoutingContext,
    fn: suspend () -> Unit,
  ) {
    try {
      fn()
    } catch (e: Throwable) {
      routingContext.fail(e)
    }
  }
}
