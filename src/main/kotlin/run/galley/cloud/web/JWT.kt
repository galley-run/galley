package run.galley.cloud.web

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.User
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import nl.clicqo.ext.getUUID
import nl.clicqo.system.Debug
import run.galley.cloud.crew.CrewAccess
import java.util.UUID

object JWT {
  fun authConfig(config: JsonObject): JWTAuthOptions =
    JWTAuthOptions()
      .setJWTOptions(jwtOptions)
      .setKeyStore(
        KeyStoreOptions()
          .setType(config.getJsonObject("jwt", JsonObject()).getString("type", "pkcs12"))
          .setPath(config.getJsonObject("jwt", JsonObject()).getString("keystore", "keystore.p12"))
          .setPassword(config.getJsonObject("jwt", JsonObject()).getString("secret", "")),
      )

  fun authProvider(
    vertx: Vertx,
    config: JsonObject,
  ): JWTAuth = JWTAuth.create(vertx, authConfig(config))

  // Base JWT Options
  private val jwtOptions =
    JWTOptions()
      .addAudience("run.galley.api")
      .setIssuer("run.galley.auth")
      .setAlgorithm("HS512")

  // Keep a short expiration time for access tokens
  val TTL_ACCESS_TOKEN: Int =
    Debug.getProperty("jwt.ttl.access")?.toIntOrNull()
      ?: 30

  // Keep a long expiration time for access tokens
  const val TTL_REFRESH_TOKEN = 7776000 // 90 days

  fun accessToken(userId: String): JWTOptions =
    jwtOptions
      .setSubject(userId)
      .setExpiresInSeconds(TTL_ACCESS_TOKEN)

  fun refreshToken(userId: String): JWTOptions =
    jwtOptions
      .setSubject(userId)
      .setExpiresInSeconds(TTL_REFRESH_TOKEN)

  fun claims(vesselId: UUID): JsonObject = JsonObject().put("vesselId", vesselId.toString())
}

fun JWTAuth.issueAccessToken(
  userId: UUID,
  crewAccess: List<CrewAccess>,
  extraClaims: JsonObject = JsonObject(),
): String =
  generateToken(
    JsonObject().mergeIn(extraClaims).put(
      "scp",
      JsonObject().apply {
        crewAccess.forEach {
          this.mergeIn(it.toJson())
        }
      },
    ),
    JWT.accessToken(userId.toString()),
  )

fun JWTAuth.issueRefreshToken(
  userId: UUID,
  extraClaims: JsonObject = JsonObject(),
): String = generateToken(extraClaims, JWT.refreshToken(userId.toString()))

fun User.getVesselId(): UUID? = this.principal().getUUID("vesselId")
