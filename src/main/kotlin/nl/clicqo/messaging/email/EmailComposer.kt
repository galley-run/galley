package nl.clicqo.messaging.email

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class EmailComposer(
  val to: Recipients,
  val subject: String,
  val template: String,
  val variables: JsonObject = JsonObject(),
  val from: String? = null,
  val cc: Recipients = Recipients.none(),
  val bcc: Recipients = Recipients.none(),
) {
  fun build(): JsonObject =
    JsonObject()
      .put("to", JsonArray(to.toList()))
      .put("cc", JsonArray(cc.toList()))
      .put("bcc", JsonArray(bcc.toList()))
      .put("subject", subject)
      .put("template", template)
      .put("variables", variables)
      .run {
        if (from != null) {
          this.put("from", from)
        }
        this
      }

  companion object {
    fun from(json: JsonObject): EmailComposer =
      EmailComposer(
        to = Recipients.from(json.getJsonArray("to")),
        subject = json.getString("subject"),
        template = json.getString("template"),
        variables = json.getJsonObject("variables"),
        from = json.getString("from"),
        cc = Recipients.from(json.getJsonArray("cc")),
        bcc = Recipients.from(json.getJsonArray("bcc")),
      )
  }
}
