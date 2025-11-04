package nl.clicqo.messaging.email

import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject

class EmailComposerCodec : MessageCodec<EmailComposer, EmailComposer> {
  override fun encodeToWire(
    buffer: Buffer,
    emailComposer: EmailComposer,
  ) {
    val json = emailComposer.build()

    val jsonString = json.encode()
    buffer.appendInt(jsonString.length)
    buffer.appendString(jsonString)
  }

  override fun decodeFromWire(
    pos: Int,
    buffer: Buffer,
  ): EmailComposer {
    var position = pos
    val length = buffer.getInt(position)
    position += 4
    val jsonString = buffer.getString(position, position + length)
    val json = JsonObject(jsonString)

    return EmailComposer.from(json)
  }

  override fun transform(emailComposer: EmailComposer): EmailComposer = emailComposer

  override fun name(): String = "email-composer"

  override fun systemCodecID(): Byte = -1
}
