package run.galley.cloud

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import run.galley.cloud.model.UserRole
import run.galley.cloud.web.issueAccessToken
import java.util.UUID

object TestJWTHelper {
  fun generateAccessToken(
    jwtAuth: JWTAuth,
    userId: UUID,
    vesselId: UUID,
    userRole: UserRole = UserRole.VESSEL_CAPTAIN,
    charterIds: List<UUID> = emptyList(),
  ): String {
    val claims =
      JsonObject()
        .put("vesselId", vesselId.toString())
        .put("charterIds", charterIds.map { it.toString() })

    return jwtAuth.issueAccessToken(userId, userRole, claims)
  }

  fun generateVesselCaptainToken(
    jwtAuth: JWTAuth,
    userId: UUID = UUID.randomUUID(),
    vesselId: UUID = UUID.randomUUID(),
  ): String = generateAccessToken(jwtAuth, userId, vesselId, UserRole.VESSEL_CAPTAIN)

  fun generateCharterCaptainToken(
    jwtAuth: JWTAuth,
    userId: UUID = UUID.randomUUID(),
    vesselId: UUID = UUID.randomUUID(),
    charterIds: List<UUID>,
  ): String = generateAccessToken(jwtAuth, userId, vesselId, UserRole.CHARTER_CAPTAIN, charterIds)
}
