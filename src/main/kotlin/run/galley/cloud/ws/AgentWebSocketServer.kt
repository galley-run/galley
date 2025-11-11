package run.galley.cloud.ws

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.http.ServerWebSocketHandshake
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import nl.clicqo.ext.fromBase64
import nl.clicqo.ext.getUUID
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class AgentWebSocketServer(
  val vertx: Vertx,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  enum class ConnectionStatus {
    CONNECTING,
    ACTIVE,
    DISCONNECTING,
    SHUTTING_DOWN,
  }

  private data class AgentConnection(
    val connectionId: String,
    val ws: ServerWebSocket,
    var credits: Int,
    val inflight: MutableSet<String> = mutableSetOf(),
    var lastPingAt: Long = System.currentTimeMillis(),
    var status: ConnectionStatus = ConnectionStatus.CONNECTING,
  )

  private data class AgentSession(
    val connections: ConcurrentHashMap<String, AgentConnection> = ConcurrentHashMap(),
  )

  // vesselEngineId -> session with multiple connections
  private val sessions = ConcurrentHashMap<String, AgentSession>()

  // vesselEngineId -> pending commands (JSON-ready maps)
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

  fun getSessionSocket(vesselEngineId: UUID): ServerWebSocket? {
    val agentSession = sessions[vesselEngineId.toString()] ?: return null
    // Return first active connection found
    return agentSession.connections.values
      .firstOrNull { it.status == ConnectionStatus.ACTIVE }
      ?.ws
  }

  fun connectionHandler(): Handler<ServerWebSocket> {
    return Handler { ws ->
      val vesselEngineId = ws.headers().get("X-Vessel-Engine-Id") ?: UUID.randomUUID().toString()
      val connectionId = ws.headers().get("X-Session-Id") ?: UUID.randomUUID().toString()
      val remoteAddress = ws.remoteAddress()

      logger.info("[WS] New connection: vesselEngineId=$vesselEngineId, connectionId=$connectionId from $remoteAddress")

      // Get or create agent session
      val agentSession = sessions.computeIfAbsent(vesselEngineId) { AgentSession() }

      val conn =
        AgentConnection(
          connectionId = connectionId,
          ws = ws,
          credits = 0,
          status = ConnectionStatus.CONNECTING,
        )
      agentSession.connections[connectionId] = conn

      logger.info("[WS] Total connections for vesselEngineId=$vesselEngineId: ${agentSession.connections.size}")

      fun drainQueue() {
        val q = queues[vesselEngineId] ?: return
        var drained = 0
        while (conn.credits > 0 && q.peek() != null) {
          val m = q.poll()
          val id = m["id"] as String
          conn.credits -= 1
          conn.inflight += id
          ws.writeTextMessage(Json.encode(m))
          drained++
        }
        if (drained > 0) {
          logger.info("[WS] Drained $drained messages to connectionId=$connectionId (credits left: ${conn.credits})")
        }
      }

      ws.textMessageHandler { text ->
        val obj = JsonObject(text)
        val msgType = obj.getString("type")
        logger.info("[WS] Received from vesselEngineId=$vesselEngineId, connectionId=$connectionId: type=$msgType")

        when (msgType) {
          "agent.hello" -> {
            val credits = obj.getJsonObject("payload")?.getInteger("credits") ?: 0
            conn.credits += credits
            conn.status = ConnectionStatus.ACTIVE
            logger.info("[WS] agent.hello from connectionId=$connectionId with $credits credits (total: ${conn.credits})")
            drainQueue()
          }

          "agent.credits" -> {
            val delta = obj.getJsonObject("payload")?.getInteger("delta") ?: 0
            conn.credits += delta
            logger.info("[WS] agent.credits from connectionId=$connectionId: delta=$delta (total: ${conn.credits})")
            drainQueue()
          }

          "cmd.done" -> {
            val vesselEngineId =
              try {
                obj.getUUID("id") ?: throw Exception("vesselEngineId not given")
              } catch (e: Exception) {
                logger.warn("[WS] Error while replying and parsing the vesselEngineId", e)
                return@textMessageHandler
              }

            conn.inflight.remove(vesselEngineId.toString())
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

      ws.shutdownHandler {
        conn.status = ConnectionStatus.SHUTTING_DOWN
        agentSession.connections.remove(connectionId)

        // Clean up agent session if no more connections
        if (agentSession.connections.isEmpty()) {
          sessions.remove(vesselEngineId)
          logger.info("[WS] All connections closed for vesselEngineId=$vesselEngineId, session removed")
        } else {
          logger.info("[WS] Connection closed: connectionId=$connectionId, remaining connections: ${agentSession.connections.size}")
        }
      }

      ws.closeHandler {
        conn.status = ConnectionStatus.DISCONNECTING
        agentSession.connections.remove(connectionId)

        // Clean up agent session if no more connections
        if (agentSession.connections.isEmpty()) {
          sessions.remove(vesselEngineId)
          logger.info("[WS] All connections closed for vesselEngineId=$vesselEngineId, session removed")
        } else {
          logger.info("[WS] Connection closed: connectionId=$connectionId, remaining connections: ${agentSession.connections.size}")
        }
      }

      ws.exceptionHandler { error ->
        logger.error("[WS] Exception for connectionId=$connectionId: ${error.message}")
        conn.status = ConnectionStatus.DISCONNECTING
        // op WS-fout sluiten we netjes, sessie wordt in closeHandler opgeruimd
        try {
          ws.close()
        } catch (_: Throwable) {
        }
      }
    }
  }
}
