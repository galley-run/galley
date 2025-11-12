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
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.CrewAccess
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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

  fun hashRefreshToken(
    tokenRaw: String,
    config: JsonObject,
  ): String {
    val pepper = config.getJsonObject("jwt")?.getString("pepper") ?: throw ApiStatus.JWT_PEPPER_MISSING
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(pepper.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    val bytes = mac.doFinal(tokenRaw.toByteArray(StandardCharsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

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
  const val TTL_NODE_AGENT = 600 // 10 minutes

  fun accessToken(userId: UUID): JWTOptions =
    jwtOptions
      .setSubject(userId.toString())
      .setExpiresInSeconds(TTL_ACCESS_TOKEN)

  fun refreshToken(userId: UUID): JWTOptions =
    jwtOptions
      .setSubject(userId.toString())
      .setExpiresInSeconds(TTL_REFRESH_TOKEN)

  fun galleyNodeAgentToken(vesselEngineNodeId: UUID): JWTOptions =
    jwtOptions
      .setSubject(vesselEngineNodeId.toString())
      .setExpiresInSeconds(TTL_NODE_AGENT)

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
    JWT.accessToken(userId),
  )

fun JWTAuth.issueRefreshToken(
  userId: UUID,
  extraClaims: JsonObject = JsonObject(),
): String = generateToken(extraClaims, JWT.refreshToken(userId))

fun JWTAuth.issueGalleyNodeAgentToken(
  vesselEngineNodeId: UUID,
  extraClaims: JsonObject = JsonObject(),
): String = generateToken(extraClaims, JWT.galleyNodeAgentToken(vesselEngineNodeId))

fun User.getVesselId(): UUID? = this.principal().getUUID("vesselId")
