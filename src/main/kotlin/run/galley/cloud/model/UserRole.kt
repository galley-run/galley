package run.galley.cloud.model

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import java.util.UUID

enum class UserRole {
  VESSEL_CAPTAIN,
  VESSEL_MEMBER,
  CHARTER_CAPTAIN,
  CHARTER_BOATSWAIN,
  CHARTER_PURSER,
  CHARTER_STEWARD,
  CHARTER_DECKHAND,
}

fun User.getCrewAccess(): JsonObject? {
  return principal().getJsonObject("scp") // ?.map(CrewAccess::valueOf)
}

fun User.getCrewAccess(vesselId: UUID): UserRole = UserRole.VESSEL_CAPTAIN
