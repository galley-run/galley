package run.galley.cloud

import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
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

  fun generateRefreshToken(
    jwtAuth: JWTAuth,
    userId: UUID,
    crewAccess: List<CrewAccess>,
  ): String =
    jwtAuth.generateToken(
      JsonObject().put(
        "scp",
        JsonObject().apply {
          crewAccess.forEach {
            this.mergeIn(it.toJson())
          }
        },
      ),
      JWTOptions()
        .addAudience("run.galley.api")
        .setIssuer("run.galley.auth")
        .setAlgorithm("HS512")
        .setSubject(userId.toString())
        .setExpiresInSeconds(90),
    )
}
