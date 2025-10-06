package nl.clicqo.ext

import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import nl.clicqo.api.ApiResponse
import nl.clicqo.api.ApiResponseOptions
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusReplyException
import org.slf4j.LoggerFactory

fun Router.setupCorsHandler(config: JsonObject): Router {
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

  return this
}

fun Router.setupDefaultOptionsHandler(): Router {
  options().handler {
    ApiResponse(it).end()
  }
  return this
}

fun Router.setupFailureHandler(): Router {
  val logger = LoggerFactory.getLogger(this::class.java)

  route().failureHandler {
    val error =
      when (it.failure()) {
        is ClassCastException -> ApiStatus.HTTP_CLASS_CAST_EXCEPTION
        else -> it.failure()
      }

    val apiResponseOptions = ApiResponseOptions(contentType = "application/vnd.galley.v1+json")

    when (error) {
      is ApiStatus -> error.takeIf { apiStatus -> apiStatus == ApiStatus.THROWABLE_EXCEPTION }?.message?.run {
        logger.error(
          this,
          error
        )
      }

      is ApiStatusReplyException -> {
        if (error.apiStatus == ApiStatus.THROWABLE_EXCEPTION) {
          logger.error(error.message, error)
        }
      }

      else -> logger.error(error.message, error)
    }

    ApiResponse(
      it,
      apiResponseOptions
    )
      .addError(
        when (error) {
          is ApiStatus -> error
          is ApiStatusReplyException -> error.apiStatus
          else -> ApiStatus.FAILED
        }
      )
      .end()
  }

  return this
}

fun Router.setupDefaultResponse(): Router {
  route().handler { ctx ->
    ctx.response()
      // Do not allow proxies to cache the data
      .putHeader("Cache-Control", "no-store, no-cache")
      // Prevents Internet Explorer from MIME - sniffing a
      // response away from the declared content-type
      .putHeader("X-Content-Type-Options", "nosniff")
      // Strict HTTPS (for about ~6Months)
      .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
      // IE8+ do not allow opening of attachments in the context of this resource
      .putHeader("X-Download-Options", "noopen")
      // Enable XSS for IE
      .putHeader("X-XSS-Protection", "1; mode=block")
      // Deny frames
      .putHeader("X-FRAME-OPTIONS", "DENY")
  }
  return this
}
