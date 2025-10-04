package nl.clicqo.ext

import io.vertx.ext.mail.MailMessage
import kotlin.collections.joinToString

fun MailMessage.printDebug() {
  println("-----------------------------------------")
  println("To: ${to.joinToString(", ")}")
  cc?.let { println("CC: ${it.joinToString(", ")}") }
  bcc?.let { println("BCC: ${it.joinToString(", ")}") }
  subject?.let { println("Subject: $it") }
  text?.let { println("Body: $it") }
  println("-----------------------------------------")
  println("")
}
