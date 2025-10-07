package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import java.util.UUID
import nl.clicqo.ext.getUUID
import run.galley.cloud.model.UserRole

object JWT {
  fun authConfig(config: JsonObject): JWTAuthOptions = JWTAuthOptions()
    .setKeyStore(
      KeyStoreOptions()
        .setType(config.getJsonObject("jwt", JsonObject()).getString("type", "jceks"))
        .setPath(config.getJsonObject("jwt", JsonObject()).getString("keystore", "keystore.jceks"))
        .setPassword(config.getJsonObject("jwt", JsonObject()).getString("secret", ""))
    )

  fun authProvider(vertx: Vertx, config: JsonObject): JWTAuth = JWTAuth.create(vertx, authConfig(config))

  // Base JWT Options
  private
  val jwtOptions = JWTOptions()
    .addAudience("run.galley.api")
    .setIssuer("run.galley.auth")

  // Keep a short expiration time for access tokens
  const
  val TTL_ACCESS_TOKEN = 30 // 30 seconds

  // Keep a long expiration time for access tokens
  const
  val TTL_REFRESH_TOKEN = 7776000 // 90 days

  fun accessToken(userId: String): JWTOptions {
    return jwtOptions
      .setSubject(userId)
      .setHeader(JsonObject().put("typ", "at+jwt"))
      .setExpiresInSeconds(TTL_ACCESS_TOKEN)

  }

  fun refreshToken(userId: String): JWTOptions {
    return jwtOptions
      .setSubject(userId)
      .setHeader(JsonObject().put("typ", "rt+jwt"))
      .setExpiresInSeconds(TTL_REFRESH_TOKEN)
  }

  fun claims(vesselId: UUID): JsonObject = JsonObject().put("vesselId", vesselId.toString())
}

fun JWTAuth.issueAccessToken(userId: String, userRole: UserRole, extraClaims: JsonObject = JsonObject()): String =
  generateToken(JsonObject().put("scope", userRole.name).mergeIn(extraClaims), JWT.accessToken(userId))

fun JWTAuth.issueRefreshToken(userId: String, userRole: UserRole, extraClaims: JsonObject = JsonObject()): String =
  generateToken(JsonObject().put("scope", userRole.name).mergeIn(extraClaims), JWT.refreshToken(userId))

fun JWTAuth.issueAccessToken(userId: UUID, userRole: UserRole, extraClaims: JsonObject = JsonObject()): String =
  generateToken(JsonObject().put("scope", userRole.name).mergeIn(extraClaims), JWT.accessToken(userId.toString()))

fun JWTAuth.issueRefreshToken(userId: UUID, userRole: UserRole, extraClaims: JsonObject = JsonObject()): String =
  generateToken(JsonObject().put("scope", userRole.name).mergeIn(extraClaims), JWT.refreshToken(userId.toString()))

fun User.getVesselId(): UUID? = this.principal().getUUID("vesselId")
