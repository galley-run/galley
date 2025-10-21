package run.galley.cloud.crew

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

fun User.getScopes(): JsonObject? = principal().getJsonObject("scp")

fun User.getUserRole(
  identifier: UUID,
  mode: CrewAccessLevel = CrewAccessLevel.VESSEL,
): UserRole? = this.getScopes()?.getString("${mode.name.lowercase()}:$identifier")?.let(UserRole::valueOf)
