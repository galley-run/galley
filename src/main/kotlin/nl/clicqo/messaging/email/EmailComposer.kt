package nl.clicqo.messaging.email

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

class EmailComposer(
  val vertx: Vertx,
) {
  lateinit var to: Recipients
  var cc: Recipients = Recipients.none()
  var bcc: Recipients = Recipients.none()
  lateinit var subject: String
  lateinit var template: String
  var variables: JsonObject = JsonObject()
  var from: String? = null

  fun build(): JsonObject =
    JsonObject()
      .put("to", to.toList())
      .put("cc", cc.toList())
      .put("bcc", bcc.toList())
      .put("subject", subject)
      .put("template", template)
      .put("variables", variables)
      .run {
        if (from != null) {
          this.put("from", from)
        }
        this
      }
}
