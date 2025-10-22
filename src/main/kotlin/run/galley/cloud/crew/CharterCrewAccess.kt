package run.galley.cloud.crew

import io.vertx.core.json.JsonObject
import java.util.UUID

data class CharterCrewAccess(
  val vesselId: UUID,
  val charterId: UUID,
  val role: CrewRole,
) : CrewAccess() {
  override fun toJson(): JsonObject = JsonObject().put("$vesselId:$charterId", role)
}
