package run.galley.cloud.model

import io.vertx.core.json.JsonObject
import run.galley.cloud.crew.CrewAccess
import run.galley.cloud.crew.CrewRole
import java.util.UUID

data class VesselCrewAccess(
  val vesselId: UUID,
  val role: CrewRole,
) : CrewAccess() {
  override fun toJson(): JsonObject = JsonObject().put(vesselId.toString(), role)
}
