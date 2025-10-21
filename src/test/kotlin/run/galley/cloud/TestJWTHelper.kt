package run.galley.cloud

import io.vertx.ext.auth.jwt.JWTAuth
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewAccess
import run.galley.cloud.crew.UserRole
import run.galley.cloud.model.VesselCrewAccess
import run.galley.cloud.web.issueAccessToken
import java.util.UUID

object TestJWTHelper {
  fun generateAccessToken(
    jwtAuth: JWTAuth,
    userId: UUID,
    crewAccess: List<CrewAccess>,
  ): String = jwtAuth.issueAccessToken(userId, crewAccess)

  fun generateVesselCaptainToken(
    jwtAuth: JWTAuth,
    userId: UUID = UUID.randomUUID(),
    vesselId: UUID = UUID.randomUUID(),
  ): String = generateAccessToken(jwtAuth, userId, listOf(VesselCrewAccess(vesselId, UserRole.VESSEL_CAPTAIN)))

  fun generateCharterCaptainToken(
    jwtAuth: JWTAuth,
    userId: UUID = UUID.randomUUID(),
    charterIds: List<UUID>,
  ): String =
    generateAccessToken(
      jwtAuth,
      userId,
      charterIds
        .map {
          CharterCrewAccess(it, UserRole.CHARTER_CAPTAIN)
        }.toList(),
    )
}
