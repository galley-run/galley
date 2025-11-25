package run.galley.cloud.ws

import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import nl.clicqo.ext.fromBase64
import nl.clicqo.ext.getUUID
import run.galley.cloud.ws.AgentWebSocketServer.ConnectionStatus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

data class AgentConnection(
  val vertx: Vertx,
  val connectionId: String,
  val ws: WebSocket,
  var credits: Int,
  val vesselEngineId: UUID,
  val inflight: MutableSet<String> = mutableSetOf(),
  var lastPingAt: Long = System.currentTimeMillis(),
  var status: ConnectionStatus = ConnectionStatus.CONNECTING,
) {
  // vesselEngineId -> pending commands (JSON-ready maps)
  private val queues = ConcurrentHashMap<String, ConcurrentLinkedQueue<Map<String, Any?>>>()

  private val logger = LoggerFactory.getLogger(this::class.java)

  fun textMessageHandler(text: String) {
    val obj = JsonObject(text)
    val msgType = obj.getString("type")
    logger.info("[WS] Received from vesselEngineId=$vesselEngineId, connectionId=$connectionId: type=$msgType")

    when (msgType) {
      "agent.hello" -> {
        val credits = obj.getJsonObject("payload")?.getInteger("credits") ?: 0
        this.credits += credits
        this.status = ConnectionStatus.ACTIVE
        logger.info("[WS] agent.hello from connectionId=$connectionId with $credits credits (total: ${this.credits})")
        drainQueue()
      }

      "agent.credits" -> {
        val delta = obj.getJsonObject("payload")?.getInteger("delta") ?: 0
        this.credits += delta
        logger.info("[WS] agent.credits from connectionId=$connectionId: delta=$delta (total: ${this.credits})")
        drainQueue()
      }

      "cmd.done" -> {
        val vesselEngineId =
          try {
            obj.getUUID("id") ?: throw Exception("vesselEngineId not given")
          } catch (e: Exception) {
            logger.warn("[WS] Error while replying and parsing the vesselEngineId", e)
            return
          }

        this.inflight.remove(vesselEngineId.toString())
        val replyTo = obj.getString("action")

        logger.info("[WS] cmd.done from connectionId=$connectionId: vesselEngineId=$vesselEngineId for $replyTo")

        try {
          vertx.eventBus().send(
            replyTo,
            EventBusAgentResponse(
              vesselEngineId = vesselEngineId,
              payload = JsonObject(obj.getString("result").fromBase64()),
            ),
          )
        } catch (e: Exception) {
          logger.warn("[WS] Error while replying", e)
        }

        // hier kun je doorzetten naar je orchestrator of audit log
      }

      // eventueel "cmd.error", "agent.metrics", ... afhandelen
      else -> {
        logger.warn("[WS] Unknown message type '$msgType' from connectionId=$connectionId")
      }
    }
  }

  fun drainQueue() {
    val q = queues[vesselEngineId.toString()] ?: return
    var drained = 0
    while (this.credits > 0 && q.peek() != null) {
      val m = q.poll()
      val id = m["id"] as String
      this.credits -= 1
      this.inflight += id
      ws.writeTextMessage(Json.encode(m))
      drained++
    }
    if (drained > 0) {
      logger.info("[WS] Drained $drained messages to connectionId=$connectionId (credits left: ${this.credits})")
    }
  }
}
