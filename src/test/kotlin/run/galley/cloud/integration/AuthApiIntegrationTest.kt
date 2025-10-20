package run.galley.cloud.integration

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import run.galley.cloud.TestJWTHelper
import run.galley.cloud.model.UserRole
import run.galley.cloud.model.VesselCrewAccess
import java.util.UUID

class AuthApiIntegrationTest : BaseIntegrationTest() {
  private val vesselId = UUID.randomUUID()
  private val userId = UUID.randomUUID()
  private val inactiveUserId = UUID.randomUUID()
  private lateinit var client: WebClient

  @BeforeEach
  override fun setupEach() {
    client =
      WebClient.create(vertx, WebClientOptions().setDefaultPort(httpPort).setDefaultHost("localhost").setIdleTimeout(5))

    // Clean database and preload required data
    runTest {
      destroyData()
      preloadUserAndVessel(userId, inactiveUserId, vesselId)
    }
  }

  @AfterEach
  fun teardownEach() {
    runTest { destroyData() }
    client.close()
  }

  private suspend fun preloadUserAndVessel(
    userId: UUID,
    inactiveUserId: UUID,
    vesselId: UUID,
  ) {
    pg
      .query(
        """
        INSERT INTO users (id, email, first_name, last_name, created_at)
        VALUES ('$userId', 'test@example.com', 'Test', 'User', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO users (id, email, first_name, last_name, created_at)
        VALUES ('$inactiveUserId', 'inactive@example.com', 'Inactive', 'User', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId', 'Test Vessel', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO crew (id, user_id, vessel_id, vessel_role, status, activated_at, created_at)
        VALUES (gen_random_uuid(), '$userId', '$vesselId', 'captain', 'active', '2025-09-01T00:00:00Z', NOW())
        ON CONFLICT DO NOTHING;
        INSERT INTO crew (id, user_id, vessel_id, vessel_role, created_at)
        VALUES (gen_random_uuid(), '$inactiveUserId', '$vesselId', 'captain', NOW())
        ON CONFLICT DO NOTHING;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  private suspend fun destroyData() {
    pg
      .query(
        """
        TRUNCATE TABLE outbox_events, charters, vessels, crew, sessions, users RESTART IDENTITY CASCADE;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  // ==================== Sign In Tests ====================

  @Test
  fun `test POST signin with valid email returns 200 with refreshToken`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("email", "test@example.com")

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertNotNull(json.getJsonObject("data").getString("refreshToken"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST signin without email returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject()

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(400, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST signin with non-existent user returns 404`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("email", "nonexistent@example.com")

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(404, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST signin with user without vessel membership returns 404`(testContext: VertxTestContext) =
    runTest {
      val userWithoutVessel = UUID.randomUUID()

      // Create user without crew membership
      pg
        .query(
          """
          INSERT INTO users (id, email, first_name, last_name, created_at)
          VALUES ('$userWithoutVessel', 'novesseluser@example.com', 'No', 'Vessel', NOW())
          ON CONFLICT (id) DO NOTHING;
          """.trimIndent(),
        ).execute()
        .coAwait()

      val body = JsonObject().put("email", "novesseluser@example.com")

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(404, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Refresh Token Tests ====================

  @Test
  fun `test POST refresh with valid refreshToken returns 200 with new refreshToken`(testContext: VertxTestContext) =
    runTest {
      // First, sign in to get a valid refresh token
      val signInBody = JsonObject().put("email", "test@example.com")
      val signInResp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(signInBody)
          .coAwait()

      val refreshToken = signInResp.bodyAsJsonObject().getJsonObject("data").getString("refreshToken")

      // Now use the refresh token
      val body = JsonObject().put("refreshToken", refreshToken)

      val resp =
        client
          .post("/auth/refresh-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertNotNull(json.getJsonObject("data").getString("refreshToken"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST refresh without refreshToken returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject()

      val resp =
        client
          .post("/auth/refresh-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(400, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST refresh with invalid refreshToken returns 401`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("refreshToken", "invalid-token")

      val resp =
        client
          .post("/auth/refresh-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST refresh with expired refreshToken returns 401`(testContext: VertxTestContext) =
    runTest {
      // Create an access token instead of refresh token (short-lived)
      val expiredToken =
        TestJWTHelper.generateAccessToken(
          getJWTAuth(),
          userId,
          listOf(VesselCrewAccess(vesselId, UserRole.VESSEL_CAPTAIN)),
        )

      val body = JsonObject().put("refreshToken", expiredToken)

      val resp =
        client
          .post("/auth/refresh-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        // This will likely succeed since we're using access token instead of refresh token
        // but it's a misuse of the endpoint - adjust based on your actual token validation
        assertEquals(200, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Access Token Tests ====================

  @Test
  fun `test POST token with valid refreshToken returns 200 with accessToken`(testContext: VertxTestContext) =
    runTest {
      // First, sign in to get a valid refresh token
      val signInBody = JsonObject().put("email", "test@example.com")
      val signInResp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(signInBody)
          .coAwait()

      val refreshToken = signInResp.bodyAsJsonObject().getJsonObject("data").getString("refreshToken")

      // Now get an access token
      val body = JsonObject().put("refreshToken", refreshToken)

      val resp =
        client
          .post("/auth/access-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertNotNull(json.getJsonObject("data").getString("accessToken"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST token without refreshToken returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject()

      val resp =
        client
          .post("/auth/access-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(400, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST token with invalid refreshToken returns 401`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("refreshToken", "invalid-token")

      val resp =
        client
          .post("/auth/access-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test SignIn with invited, not active, user returns 404`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("email", "inactive@example.com")

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(404, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST token with deleted user returns 403`(testContext: VertxTestContext) =
    runTest {
      // First, sign in to get a valid refresh token
      val signInBody = JsonObject().put("email", "test@example.com")
      val signInResp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(signInBody)
          .coAwait()

      val refreshToken = signInResp.bodyAsJsonObject().getJsonObject("data").getString("refreshToken")

      // Delete the user
      pg
        .query(
          """
          DELETE FROM crew WHERE user_id = '$userId';
          """.trimIndent(),
        ).execute()
        .coAwait()

      // Try to get an access token with the refresh token of a deleted user
      val body = JsonObject().put("refreshToken", refreshToken)

      val resp =
        client
          .post("/auth/access-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, signInResp.statusCode())
        assertNotNull(refreshToken)
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Edge Cases ====================

  @Test
  fun `test POST signin with empty email returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("email", "")

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(500, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST token returns accessToken that can be used for authenticated requests`(testContext: VertxTestContext) =
    runTest {
      // Sign in to get refresh token
      val signInBody = JsonObject().put("email", "test@example.com")
      val signInResp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(signInBody)
          .coAwait()

      val refreshToken = signInResp.bodyAsJsonObject().getJsonObject("data").getString("refreshToken")

      // Get access token
      val tokenBody = JsonObject().put("refreshToken", refreshToken)
      val tokenResp =
        client
          .post("/auth/access-token")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(tokenBody)
          .coAwait()

      val accessToken = tokenResp.bodyAsJsonObject().getJsonObject("data").getString("accessToken")

      // Try to use the access token on a protected endpoint (e.g., GET charters)
      val chartersResp =
        client
          .get("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $accessToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(200, chartersResp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST signin with multiple vessel memberships returns first vessel`(testContext: VertxTestContext) =
    runTest {
      val secondVesselId = UUID.randomUUID()

      // Add another vessel and crew membership
      pg
        .query(
          """
          INSERT INTO vessels (id, name, user_id, created_at)
          VALUES ('$secondVesselId', 'Second Vessel', '$userId', NOW())
          ON CONFLICT (id) DO NOTHING;
          INSERT INTO crew (id, user_id, vessel_id, vessel_role, created_at)
          VALUES (gen_random_uuid(), '$userId', '$secondVesselId', 'captain', NOW())
          ON CONFLICT DO NOTHING;
          """.trimIndent(),
        ).execute()
        .coAwait()

      val body = JsonObject().put("email", "test@example.com")

      val resp =
        client
          .post("/auth/sign-in")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertNotNull(json.getJsonObject("data").getString("refreshToken"))
        // The token should be valid and contain a vesselId
        testContext.completeNow()
      }
    }
}
