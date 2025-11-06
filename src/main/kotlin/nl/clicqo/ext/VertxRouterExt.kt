package nl.clicqo.ext

import io.vertx.core.eventbus.ReplyException
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.HttpException
import io.vertx.json.schema.JsonSchemaValidationException
import io.vertx.openapi.validation.SchemaValidationException
import io.vertx.openapi.validation.ValidatorErrorType
import io.vertx.openapi.validation.ValidatorException
import nl.clicqo.api.ApiErrorSource
import nl.clicqo.api.ApiResponse
import nl.clicqo.api.ApiResponseOptions
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusReplyException
import org.slf4j.LoggerFactory

fun Router.setupCorsHandler(corsConfig: JsonArray): Router {
  route()
    .handler(
      CorsHandler
        .create()
        .addOrigins(corsConfig.map { it as String })
        .allowCredentials(true)
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
    var source: ApiErrorSource? = null
    var error =
      when (it.failure()) {
        is ClassCastException -> ApiStatus.HTTP_CLASS_CAST_EXCEPTION
        else -> it.failure()
      }

    val apiResponseOptions = ApiResponseOptions(contentType = "application/vnd.galley.v1+json")

    when (error) {
      is ApiStatus ->
        error.takeIf { apiStatus -> apiStatus == ApiStatus.THROWABLE_EXCEPTION }?.message?.run {
          logger.error(
            this,
            error,
          )
        }

      is JsonSchemaValidationException -> {
        error = ApiStatus.FAILED_VALIDATION
        source = ApiErrorSource().setPointer(error.message)
      }

      is ApiStatusReplyException -> {
        if (error.apiStatus == ApiStatus.THROWABLE_EXCEPTION) {
          logger.error(error.message, error)
        }
      }

      is ReplyException -> {
        logger.error(error.message, error)
        error = ApiStatus.FAILED // It can be anything
      }

      is HttpException -> {
        logger.error("HttpException", error)

        when (error.cause) {
          is SchemaValidationException -> {
            (error.cause as SchemaValidationException).outputUnit.errors.first().instanceLocation?.let { location ->
              source = ApiErrorSource().setPointer(location)
            }
            error = ApiStatus(ApiStatus.FAILED_VALIDATION, error.cause?.message)
          }

          is JsonSchemaValidationException -> {
            error = ApiStatus.FAILED_VALIDATION
            source = ApiErrorSource().setPointer((error.cause as JsonSchemaValidationException).location())
          }

          is ValidatorException -> {
            error =
              when ((error.cause as ValidatorException).type()) {
                ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT -> ApiStatus.CONTENT_TYPE_NOT_DEFINED
                else -> ApiStatus.FAILED_VALIDATION
              }
          }

          else -> {
            error =
              when (error.statusCode) {
                401 -> ApiStatus.FAILED_AUTHORIZATION
                404 -> ApiStatus.FAILED_FIND
                400 -> ApiStatus.FAILED_VALIDATION
                else -> ApiStatus.FAILED
              }
          }
        }
      }

      else -> logger.error(error.message, error)
    }

    ApiResponse(
      it,
      apiResponseOptions,
    ).addError(
      when (error) {
        is ApiStatus -> error
        is ApiStatusReplyException -> error.apiStatus
        else -> ApiStatus.FAILED
      },
      source = source,
    ).end()
  }

  return this
}

fun Router.setupDefaultResponse(): Router {
  route().handler { ctx ->
    ctx
      .response()
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
    ctx.next()
  }
  return this
}
