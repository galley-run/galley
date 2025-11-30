package run.galley.cloud.ws

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.http.ServerWebSocketHandshake
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketConnectOptions
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.clicqo.ext.toUUID
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AgentWebSocketServer(
  val vertx: Vertx,
  val outboundConnections: JsonObject? = null,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)
  private var reconnectTimerId: Long? = null
  private var backoff = 1000L

  // Overrides an inbound connection with an outbound connection on the requested vesselEngineId
  suspend fun createOutboundConnection(vesselEngineId: UUID): WebSocket? {
    if (outboundConnections == null || outboundConnections.isEmpty) {
      return null
    }

    logger.info("Creating outbound connection to $vesselEngineId")

    val ws =
      try {
        vertx
          .createWebSocketClient()
          .connect(
            WebSocketConnectOptions()
              .setURI(outboundConnections.getString(vesselEngineId.toString()))
              .addHeader("X-Vessel-Engine-Id", vesselEngineId.toString())
              .addHeader("Authorization", "Bearer dev"),
          ).coAwait()
      } catch (e: Exception) {
        logger.error("Error while connecting to $vesselEngineId", e)
        null
      }

    if (ws == null) {
      scheduleReconnect(vesselEngineId)
      return null
    }

    // Create agent session
    val connectionId = UUID.randomUUID().toString()

    val conn =
      AgentConnection(
        vertx = vertx,
        connectionId = connectionId,
        ws = ws,
        credits = 0,
        status = ConnectionStatus.ACTIVE,
        vesselEngineId = vesselEngineId,
      )
    ws.textMessageHandler(conn::textMessageHandler)
    val agentSession = AgentSession()
    agentSession.connections[connectionId] = conn

    ws.closeHandler {
      logger.info("Closing connection $connectionId")
      scheduleReconnect(vesselEngineId)
    }
    ws.shutdownHandler {
      logger.info("Shutting down connection $connectionId")
    }
    ws.exceptionHandler {
      logger.info("Uncaught exception in connection $connectionId")
      scheduleReconnect(vesselEngineId)
    }

    sessions[vesselEngineId.toString()] = agentSession

    return ws
  }

  private fun scheduleReconnect(vesselEngineId: UUID) {
    reconnectTimerId =
      vertx.setTimer(backoff) {
        val scope = CoroutineScope(vertx.dispatcher() + SupervisorJob())
        scope.launch {
          createOutboundConnection(vesselEngineId)
        }
      }
  }

  enum class ConnectionStatus {
    CONNECTING,
    ACTIVE,
    DISCONNECTING,
    SHUTTING_DOWN,
  }

  // vesselEngineId -> session with multiple connections
  private val sessions = ConcurrentHashMap<String, AgentSession>()

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

  fun getSessionSocket(vesselEngineId: UUID): WebSocket? {
    val agentSession = sessions[vesselEngineId.toString()] ?: return null
    // Return first active connection found
    return agentSession.connections.values
      .firstOrNull { it.status == ConnectionStatus.ACTIVE }
      ?.ws
  }

  fun connectionHandler(): Handler<ServerWebSocket> =
    Handler { ws ->
      val vesselEngineId = ws.headers().get("X-Vessel-Engine-Id") ?: UUID.randomUUID().toString()
      val connectionId = ws.headers().get("X-Session-Id") ?: UUID.randomUUID().toString()
      val remoteAddress = ws.remoteAddress()

      logger.info("[WS] New connection: vesselEngineId=$vesselEngineId, connectionId=$connectionId from $remoteAddress")

      // Get or create agent session
      val agentSession = sessions.computeIfAbsent(vesselEngineId) { AgentSession() }

      val conn =
        AgentConnection(
          vertx = vertx,
          connectionId = connectionId,
          ws = ws,
          credits = 0,
          status = ConnectionStatus.CONNECTING,
          vesselEngineId = vesselEngineId.toUUID(),
        )
      agentSession.connections[connectionId] = conn

      logger.info("[WS] Total connections for vesselEngineId=$vesselEngineId: ${agentSession.connections.size}")

      ws.textMessageHandler(conn::textMessageHandler)

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
