package nl.clicqo.license

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.coroutineEventBus
import io.vertx.kotlin.coroutines.setPeriodicAwait
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.system.Debug
import java.time.Instant
import java.time.OffsetDateTime

class LicenseVerticle : CoroutineVerticle() {
  var scopes: JsonArray = JsonArray()
  var license: String? = null
  var licensedTo: String? = null
  var expireAt: Instant? = null
  var alert: LicenseAlert? = null
  private var publicKeyPem: String? = null

  companion object {
    const val PUBLIC_GET = "license.query.get"
    const val GET_SCOPE = "license.query.get.scope"
    const val FETCH = "license.fetch"
  }

  override suspend fun start() {
    super.start()

    val path =
      config.getJsonObject("license", JsonObject()).getString("publicKeyPath", "license_public.pem")
    val licenseKey = config.getJsonObject("license")?.getString("key")

    if (licenseKey != null) {
      publicKeyPem =
        try {
          vertx
            .fileSystem()
            ?.readFile(path)
            ?.coAwait()
            ?.toString(Charsets.UTF_8)
        } catch (_: Exception) {
          null
        }
    }

    coroutineEventBus {
      vertx.eventBus().coConsumer(FETCH, handler = ::fetchLicense)
      vertx.eventBus().coConsumer(GET_SCOPE, handler = ::getScope)
      vertx.eventBus().coConsumer(PUBLIC_GET, handler = ::getLicensePublic)
    }

    vertx
      .eventBus()
      .request<String?>(FETCH, true)
      .coAwait()
      .body()

    vertx.setPeriodicAwait(3600000L) {
      vertx.eventBus().send(FETCH, true)
    }
  }

  private suspend fun getScope(message: Message<String>) {
    message.reply(scopes.contains(message.body()))
  }

  private suspend fun fetchLicense(message: Message<Boolean>) {
    val showLicenseMessage = message.body() ?: false

    val licenseKey = config.getJsonObject("license")?.getString("key")
    alert = null

    if (!licenseKey.isNullOrBlank() && !publicKeyPem.isNullOrBlank()) {
      try {
        val response =
          WebClient
            .create(vertx)
            .postAbs("${Debug.getProperty("license.server") ?: "https://license.galley.run"}/license/check")
            .sendJsonObject(JsonObject().put("licenseKey", licenseKey))
            ?.coAwait()

        if (response?.statusCode() == 402) {
          scopes = JsonArray()
          licensedTo = null
          license = "Expired, Payment Required"
          alert = LicenseAlert.PAYMENT_REQUIRED
        } else {
          response
            ?.bodyAsJsonObject()
            ?.let {
              val signatureToken = it.getString("signature")

              licensedTo = it.getString("name")
              license = it.getString("license")

              try {
                val user =
                  JWTAuth
                    .create(
                      vertx,
                      JWTAuthOptions()
                        .addPubSecKey(PubSecKeyOptions().setAlgorithm("RS256").setBuffer(publicKeyPem)),
                    ).authenticate(TokenCredentials(signatureToken))
                    .coAwait()

                scopes = user.principal().getJsonArray("scope")
                expireAt = OffsetDateTime.parse(it.getString("expiresAt")).toInstant()
              } catch (_: Exception) {
                if (expireAt == null || expireAt?.isBefore(Instant.now()) == true) {
                  scopes = JsonArray()
                }
              }
            }
        }
      } catch (_: Exception) {
        if (expireAt == null || expireAt?.isBefore(Instant.now()) == true) {
          scopes = JsonArray()
          licensedTo = null
          license = null
        }
      }
    }

    if (showLicenseMessage) {
      if (this.licensedTo != null) {
        println("----------------------------------------")
        println("[ GALLEY LICENSED EDITION ]")
        println("Licensed to ${this.licensedTo}")
        this.license?.run { println("License: $this") }
        println("----------------------------------------")
      } else {
        println("----------------------------------------")
        println("[ GALLEY DEVELOPER EDITION ]")
        this.license?.run { println("License: $this") }
        println("----------------------------------------")
      }
    }

    message.reply(null)
  }

  private suspend fun getLicensePublic(message: Message<EventBusApiRequest>) {
    message.reply(
      EventBusApiResponse(
        JsonObject()
          .put("license", license)
          .put("licensedTo", licensedTo)
          .put("alert", alert?.toString()?.lowercase())
          .put("scopes", scopes),
      ),
    )
  }
}

suspend fun Vertx.getScope(scope: String): Boolean =
  this
    .eventBus()
    .request<Boolean>(LicenseVerticle.GET_SCOPE, scope)
    .coAwait()
    .body()
