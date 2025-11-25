package run.galley.cloud.ws

import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import kotlin.coroutines.CoroutineContext

class VesselEngineAgentTunnel : CoroutineEventBusSupport {
  val vertx: Vertx
  val agentWebSocketServer: AgentWebSocketServer
  override val coroutineContext: CoroutineContext

  companion object {
    const val GET_NODES = "vessel-engine-agent.nodes.query.list"
    const val APPLY = "vessel-engine-agent.nodes.cmd.apply"
  }

  constructor(vertx: Vertx, agentWebSocketServer: AgentWebSocketServer, coroutineContext: CoroutineContext) {
    this.vertx = vertx
    this.agentWebSocketServer = agentWebSocketServer
    this.coroutineContext = coroutineContext

    coroutineEventBus {
      vertx.eventBus().coConsumer(GET_NODES, handler = ::getNodes)
      vertx.eventBus().coConsumer(APPLY, handler = ::apply)
    }
  }

  private suspend fun getNodes(message: Message<EventBusAgentRequest>) {
    val request = message.body()
    request.action = "k8s.nodes.get"

    agentWebSocketServer
      .getSessionSocket(request.vesselEngineId)
      ?.writeTextMessage(request.toSocketMessage())
  }

  private suspend fun apply(message: Message<EventBusAgentRequest>) {
    val request = message.body()
    request.action = "k8s.apply"

    agentWebSocketServer
      .getSessionSocket(request.vesselEngineId)
      ?.writeTextMessage(request.toSocketMessage())
  }
}
