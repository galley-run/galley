package run.galley.cloud.ws

import io.vertx.core.json.JsonObject
import java.util.UUID

data class EventBusAgentResponse(
  val vesselEngineId: UUID,
  val payload: JsonObject,
)
