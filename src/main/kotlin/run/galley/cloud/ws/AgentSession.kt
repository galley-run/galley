package run.galley.cloud.ws

import java.util.concurrent.ConcurrentHashMap

data class AgentSession(
  val connections: ConcurrentHashMap<String, AgentConnection> = ConcurrentHashMap(),
)
