package run.galley.cloud.ws

import io.vertx.core.Handler
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.http.ServerWebSocketHandshake
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class AgentWebSocketServer {
  private val logger = LoggerFactory.getLogger(this::class.java)

  private data class AgentSession(
    val ws: ServerWebSocket,
    var credits: Int,
    val inflight: MutableSet<String> = mutableSetOf(),
    var lastPingAt: Long = System.currentTimeMillis(),
  )

  // agentId -> session
  private val sessions = ConcurrentHashMap<String, AgentSession>()

  // agentId -> pending commands (JSON-ready maps)
  private val queues = ConcurrentHashMap<String, ConcurrentLinkedQueue<Map<String, Any?>>>()

  // simpele anti-abuse: IP → count in huidig tijdslot
  private val handshakesPerIp = ConcurrentHashMap<String, Int>()
  private val handshakeLimitPerMin = 60

  fun handshakeHandler(): Handler<ServerWebSocketHandshake> {
    return Handler { handshake ->
      val ip = handshake.remoteAddress().host()
      logger.info("[WS] Handshake request from $ip on path ${handshake.path()}")

      if (handshake.path() != "/agents/connect") {
        logger.info("[WS] Rejected: Invalid path ${handshake.path()}")
        handshake.reject(404)
        return@Handler
      }

      // eenvoudige IP-rate limit op handshakes
      val count = handshakesPerIp.merge(ip, 1) { a, b -> a + b } ?: 1
      if (count > handshakeLimitPerMin) {
        logger.warn("[WS] Rejected: Rate limit exceeded for $ip ($count/$handshakeLimitPerMin)")
        handshake.reject(429)
        return@Handler
      }

      // basic header-based auth (dev). In prod vervang je dit door mTLS of JWT-check op LB of hier.
      val authz = handshake.headers().get("Authorization")
      if (authz != null && !authz.startsWith("Bearer ")) {
        logger.warn("[WS] Rejected: Invalid Authorization header")
        handshake.reject(401)
        return@Handler
      }

      logger.info("[WS] Handshake accepted from $ip")
      handshake.accept()
    }
  }

  fun connectionHandler(): Handler<ServerWebSocket> {
    return Handler { ws ->
      val agentId = ws.headers().get("X-Agent-Id") ?: UUID.randomUUID().toString()
      val remoteAddress = ws.remoteAddress()

      logger.info("[WS] New connection: agentId=$agentId from $remoteAddress")

      val sess = AgentSession(ws, credits = 0)
      // één actieve sessie per agent, oude sluiten
      sessions.put(agentId, sess)?.let { old ->
        logger.info("[WS] Closing old session for agentId=$agentId")
        try {
          old.ws.close((4000..4009).random().toShort())
        } catch (_: Throwable) {
        }
      }

      fun drainQueue() {
        val q = queues[agentId] ?: return
        var drained = 0
        while (sess.credits > 0 && q.peek() != null) {
          val m = q.poll()
          val id = m["id"] as String
          sess.credits -= 1
          sess.inflight += id
          ws.writeTextMessage(Json.encode(m))
          drained++
        }
        if (drained > 0) {
          logger.info("[WS] Drained $drained messages to agentId=$agentId (credits left: ${sess.credits})")
        }
      }

      ws.textMessageHandler { text ->
        val obj = JsonObject(text)
        val msgType = obj.getString("type")
        logger.info("[WS] Received from agentId=$agentId: type=$msgType")

        when (msgType) {
          "agent.hello" -> {
            val credits = obj.getJsonObject("payload")?.getInteger("credits") ?: 0
            sess.credits += credits
            logger.info("[WS] agent.hello from agentId=$agentId with $credits credits (total: ${sess.credits})")
            drainQueue()
          }

          "agent.credits" -> {
            val delta = obj.getJsonObject("payload")?.getInteger("delta") ?: 0
            sess.credits += delta
            logger.info("[WS] agent.credits from agentId=$agentId: delta=$delta (total: ${sess.credits})")
            drainQueue()
          }

          "cmd.done" -> {
            val id = obj.getString("id")
            sess.inflight.remove(id)
            logger.info("[WS] cmd.done from agentId=$agentId: cmdId=$id")
            // hier kun je doorzetten naar je orchestrator of audit log
          }
          // eventueel "cmd.error", "agent.metrics", ... afhandelen
          else -> {
            logger.warn("[WS] Unknown message type '$msgType' from agentId=$agentId")
          }
        }
      }

      ws.closeHandler {
        sessions.remove(agentId)
        logger.info("[WS] Connection closed: agentId=$agentId")
      }

      ws.exceptionHandler { error ->
        logger.error("[WS] Exception for agentId=$agentId: ${error.message}")
        // op WS-fout sluiten we netjes, sessie wordt in closeHandler opgeruimd
        try {
          ws.close()
        } catch (_: Throwable) {
        }
      }
    }
  }
}
