package run.galley.cloud.model

import io.vertx.core.json.JsonObject
import java.util.UUID

data class CharterCrewAccess(
  val charterId: UUID,
  val role: UserRole,
) : CrewAccess() {
  override fun toJson(): JsonObject = JsonObject().put("charter:$charterId", role)
}
