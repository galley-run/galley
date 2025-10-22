package run.galley.cloud

import io.vertx.ext.auth.jwt.JWTAuth
import run.galley.cloud.crew.CrewAccess
import run.galley.cloud.web.issueAccessToken
import java.util.UUID

object TestJWTHelper {
  fun generateAccessToken(
    jwtAuth: JWTAuth,
    userId: UUID,
    crewAccess: List<CrewAccess>,
  ): String = jwtAuth.issueAccessToken(userId, crewAccess)
}
