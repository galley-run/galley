package run.galley.cloud.crew

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import nl.clicqo.ext.toUUID
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

fun User.getCharters(): List<UUID>? =
  principal()
    .getJsonObject("scp")
    .map.keys
    .mapNotNull {
      if (!it.contains(":")) {
        return@mapNotNull null
      }
      it.substringAfterLast(":").toUUID()
    }.ifEmpty { null }

fun User.getVessels(): List<UUID>? =
  principal()
    .getJsonObject("scp")
    .map.keys
    .mapNotNull {
      when {
        it.contains(":") -> it.substringBefore(":").toUUID()
        else -> it.toUUID()
      }
    }.ifEmpty { null }

fun User.getUserRole(
  vesselId: UUID,
  charterId: UUID? = null,
): UserRole? = this.getScopes()?.getString("$vesselId${charterId?.let { ":$it" } ?: ""}")?.let(UserRole::valueOf)
