package run.galley.cloud.crew

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import nl.clicqo.ext.toUUID
import java.util.UUID
import kotlin.collections.ifEmpty

fun User.getScopes(): JsonObject? = principal().getJsonObject("scp")

fun User.getCharters(vesselId: UUID? = null): List<UUID>? =
  principal()
    .getJsonObject("scp")
    .map.keys
    .mapNotNull {
      if (!it.contains(":")) {
        return@mapNotNull null
      }
      if (vesselId != null && !it.startsWith(vesselId.toString())) {
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

fun User.getCrewRole(
  vesselId: UUID,
  charterId: UUID? = null,
): CrewRole? =
  this
    .getScopes()
    ?.getString("$vesselId${charterId?.let { ":$it" } ?: ""}")
    ?.let { roleName ->
      CrewRole.valueOf(roleName)
    }
