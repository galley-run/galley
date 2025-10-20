package run.galley.cloud.model

import io.vertx.core.json.JsonObject
import java.util.UUID

data class VesselCrewAccess(
  val vesselId: UUID,
  val role: UserRole,
) : CrewAccess() {
  override fun toJson(): JsonObject = JsonObject().put("vessel:$vesselId", role)
}
