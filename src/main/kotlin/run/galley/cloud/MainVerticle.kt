package run.galley.cloud

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.APIResponse
import nl.clicqo.api.APIResponseOptions
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusException
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.ApiStatusReplyExceptionMessageCodec
import run.galley.cloud.controller.VesselControllerVerticle
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataRequestCodec
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusDataResponseCodec
import run.galley.cloud.web.OpenAPIBridge


class MainVerticle : CoroutineVerticle() {

  override suspend fun start() {

    val retriever = ConfigRetriever.create(
      vertx, ConfigRetrieverOptions()
        .addStore(ConfigStoreOptions().setType("file").setConfig(JsonObject().put("path", "config.json")))
    )

    vertx.eventBus().registerDefaultCodec(EventBusDataRequest::class.java, EventBusDataRequestCodec())
    vertx.eventBus().registerDefaultCodec(EventBusDataResponse::class.java, EventBusDataResponseCodec())
    vertx.eventBus().registerDefaultCodec(ApiStatusReplyException::class.java, ApiStatusReplyExceptionMessageCodec())

    val config = retriever.config.coAwait()

    val router = OpenAPIBridge(config).buildRouter(vertx)

    router.apply {
      route()
        .handler(
          CorsHandler
            .create()
            .addOriginsWithRegex(
              config.getJsonObject("api").getJsonArray("cors", JsonArray().add(".*")).map { it.toString() })
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.DELETE)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedMethod(HttpMethod.PATCH)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.POST)
            .allowedHeader("Origin")
            .allowedHeader("Depth")
            .allowedHeader("User-Agent")
            .allowedHeader("X-Requested-With")
            .allowedHeader("X-Permissions")
            .allowedHeader("Content-Type")
            .allowedHeader("Accept")
            .allowedHeader("Authorization")
            .allowedHeader("Cache-Control")
            .allowedHeader("X-File-Name")
            .allowedHeader("If-Modified-Since")
            .maxAgeSeconds(6000),
        )
      route().failureHandler {
        val error =
          when (it.failure()) {
            is ClassCastException -> ApiStatusException(ApiStatus.HTTP_CLASS_CAST_EXCEPTION)
            else -> it.failure()
          }

        val apiResponseOptions = APIResponseOptions(contentType = "application/vnd.galley.v1+json")

        APIResponse(
          it,
          apiResponseOptions
        )
          .addError(
            when (error) {
              is ApiStatusException -> error.apiStatus
              is ApiStatusReplyException -> error.apiStatus
              else -> ApiStatus.FAILED
            }
          )
          .end()
      }
      options().handler {
        APIResponse(it).end()
      }
    }

    // Implemmentn erro handler with exception handler codec

    val deploymentOptions = DeploymentOptions()
      .setConfig(config)

    vertx
      .deployVerticle(VesselControllerVerticle(), deploymentOptions)
      .coAwait()

    val httpPort = config
      .getJsonObject("http", JsonObject())
      .getInteger("port", 9233)
    vertx
      .createHttpServer()
      .requestHandler(router)
      .listen(httpPort).onSuccess { http ->
        println("HTTP server started on port $httpPort")
      }
      .coAwait()
  }
}
