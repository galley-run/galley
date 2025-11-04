package nl.clicqo.messaging.email

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.mail.MailClient
import io.vertx.ext.mail.MailConfig
import io.vertx.ext.mail.MailMessage
import io.vertx.ext.web.templ.pebble.PebbleTemplateEngine
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import java.time.Instant

class EmailMessagingVerticle :
  CoroutineVerticle(),
  CoroutineEventBusSupport {
  companion object Companion {
    const val SEND = "messaging.email.send"

    const val DEFAULT_POOL_NAME = "messaging.email.default-pool"
  }

  private lateinit var client: MailClient
  private var mailConfig: MailConfig? = null

  override suspend fun start() {
    super.start()

    mailConfig =
      MailConfig(config)

    client =
      MailClient
        .createShared(vertx, mailConfig, DEFAULT_POOL_NAME)

    coroutineEventBus {
      vertx.eventBus().coConsumer(SEND, handler = ::sendEmail)
    }
  }

  private suspend fun sendEmail(message: Message<JsonObject>) {
    val body = message.body()

    val to = body.getJsonArray("to").map { it.toString() }
    val cc =
      (
        body.getJsonArray("cc").addAll(
          config.getJsonObject("defaults", JsonObject()).getJsonArray(
            "cc",
            JsonArray(),
          ),
        ) ?: emptyList()
      ).map { it.toString() }
    val bcc =
      (
        body.getJsonArray("bcc").addAll(
          config.getJsonObject("defaults", JsonObject()).getJsonArray(
            "bcc",
            JsonArray(),
          ),
        ) ?: emptyList()
      ).map { it.toString() }
    val subject = body.getString("subject")
    val template = body.getString("template")
    val from = body.getString("from", config.getJsonObject("defaults", JsonObject()).getString("from", "NO FROM SET"))
    val variables =
      JsonObject()
        .put("subject", subject)
        .put("now", Instant.now())
        .mergeIn(
          JsonObject.mapFrom(
            body.getJsonObject("variables", JsonObject()).map.mapValues {
              if (it.key.lowercase().endsWith("url") &&
                !it.value.toString().startsWith("http")
              ) {
                return@mapValues "${
                  config.getJsonObject("defaults", JsonObject()).getString("urlPrefix", "")
                }${it.value}"
              }
            },
          ),
        )

    val engine = PebbleTemplateEngine.create(vertx)
    val html =
      try {
        engine.render(variables, "templates/email/$template.html").coAwait()
      } catch (e: Exception) {
        println(e.message)
      }
    val plainText =
      try {
        engine.render(variables, "templates/email/$template.text").coAwait()
      } catch (e: Exception) {
        println(e.message)
      }

    val email =
      MailMessage()
        .setTo(to)
        .setCc(cc)
        .setBcc(bcc)
        .setSubject(subject)
        .setHtml(html.toString())
        .setText(plainText.toString())
        .setFrom(from)

    val result =
      try {
        client.sendMail(email).coAwait()
      } catch (e: Exception) {
        e.printStackTrace()
        return
      }
    message.reply(result?.toJson())
  }

  override suspend fun stop() {
    super.stop()

    client.close()
  }
}
