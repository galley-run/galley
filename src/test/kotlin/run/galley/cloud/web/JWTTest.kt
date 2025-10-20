package run.galley.cloud.web

import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class JWTTest {
  @Test
  fun `authConfig uses defaults when jwt-config is missing`() {
    val config = JsonObject() // no "jwt" object

    val options = JWT.authConfig(config)
    val ks = options.keyStore

    assertNotNull(ks)
    assertEquals("pkcs12", ks.type)
    assertEquals("keystore.p12", ks.path)
    assertEquals("", ks.password)

    // Basic JWT options are also set via setJWTOptions(...)
    val o = options.jwtOptions
    // Note: issuer, audience, and alg are inherited from the private jwtOptions
    assertEquals("run.galley.auth", o.issuer)
    assertTrue(o.audience.contains("run.galley.api"))
    assertEquals("HS512", o.algorithm)
  }

  @Test
  fun `authConfig uses overrides from jwt config`() {
    val config =
      JsonObject()
        .put(
          "jwt",
          JsonObject()
            .put("type", "pkcs12")
            .put("keystore", "test-keys/keystore.p12")
            .put("secret", "changeit"),
        )

    val options = JWT.authConfig(config)
    val ks = options.keyStore

    assertEquals("pkcs12", ks.type)
    assertEquals("test-keys/keystore.p12", ks.path)
    assertEquals("changeit", ks.password)
  }

  @Test
  fun `accessToken sets subject and short TTL and inherits issuer audience alg`() {
    val userId = UUID.randomUUID().toString()

    val o = JWT.accessToken(userId)

    assertEquals(userId, o.subject)
    // default is 30 sec in the code
    assertEquals(30, o.expiresInSeconds)
    assertEquals("run.galley.auth", o.issuer)
    assertTrue(o.audience.contains("run.galley.api"))
    assertEquals("HS512", o.algorithm)
  }

  @Test
  fun `refreshToken sets subject and long TTL and inherits issuer audience alg`() {
    val userId = UUID.randomUUID().toString()

    val o = JWT.refreshToken(userId)

    assertEquals(userId, o.subject)
    // 90 days in seconds
    assertEquals(7_776_000, o.expiresInSeconds)
    assertEquals("run.galley.auth", o.issuer)
    assertTrue(o.audience.contains("run.galley.api"))
    assertEquals("HS512", o.algorithm)
  }

  @Test
  fun `claims contains vesselId as string`() {
    val vesselId = UUID.randomUUID()
    val claims = JWT.claims(vesselId)

    assertEquals(vesselId.toString(), claims.getString("vesselId"))
  }
}
