package run.galley.cloud.integration

import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import run.galley.cloud.TestJWTHelper
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewRole
import java.util.UUID

class CharterConnectionApiIntegrationTest : BaseIntegrationTest() {
  private val vesselId = UUID.randomUUID()
  private val charterId = UUID.randomUUID()
  private val userId = UUID.randomUUID()
  private lateinit var validToken: String

  @BeforeEach
  override fun setupEach() {
    super.setupEach()

    validToken =
      TestJWTHelper.generateAccessToken(
        getJWTAuth(),
        userId = userId,
        crewAccess =
          listOf(
            CharterCrewAccess(vesselId, charterId, CrewRole.CHARTER_CAPTAIN),
          ),
      )

    // Clean database and preload required data
    runTest {
      destroyData()
      preloadData()
    }
  }

  @AfterEach
  fun teardownEach() {
    runTest { destroyData() }
    client.close()
  }

  private suspend fun preloadData() {
    pg
      .query(
        """
        INSERT INTO users (id, email, first_name, last_name, created_at)
        VALUES ('$userId', 'captain@vessel.com', 'Test', 'Person', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId', 'Test Vessel', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charters (id, vessel_id, name, user_id, created_at)
        VALUES ('$charterId', '$vesselId', 'Test Charter', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  private suspend fun destroyData() {
    pg
      .query(
        """
        TRUNCATE TABLE oauth_connection_grants, oauth_credentials, oauth_connections, charter_compute_plans, charter_projects, outbox_events, charters, crew, vessels, sessions, users RESTART IDENTITY CASCADE;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  // ==================== GET List Tests ====================

  @Test
  fun `test GET connections list returns 200 with empty list when no connections exist`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        assertNotNull(json.getJsonArray("data"))
        assertEquals(0, json.getJsonArray("data").size())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list returns 200 with connections when they exist`(testContext: VertxTestContext) =
    runTest {
      // Create a connection directly in the database
      val connectionId = UUID.randomUUID()
      pg
        .query(
          """
          INSERT INTO oauth_connections (id, charter_id, vessel_id, type, provider, status, created_by_user_id, provider_account_id, created_at)
          VALUES ('$connectionId', '$charterId', '$vesselId', 'git', 'github', 'active', '$userId', '12345', NOW());
          """.trimIndent(),
        ).execute()
        .coAwait()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val data = json.getJsonArray("data")
        assertEquals(1, data.size())

        val connection = data.getJsonObject(0)
        assertEquals(connectionId.toString(), connection.getString("id"))
        assertEquals("git", connection.getString("type"))
        assertEquals("github", connection.getString("provider"))
        assertEquals("active", connection.getString("status"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list with invalid vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$differentVesselId/charters/$charterId/connections")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list with provider filter returns only matching connections`(testContext: VertxTestContext) =
    runTest {
      // Create multiple connections with different providers
      val githubConnectionId = UUID.randomUUID()
      val gitlabConnectionId = UUID.randomUUID()

      pg
        .query(
          """
          INSERT INTO oauth_connections (id, charter_id, vessel_id, type, provider, status, created_by_user_id, provider_account_id, created_at)
          VALUES
            ('$githubConnectionId', '$charterId', '$vesselId', 'git', 'github', 'active', '$userId', '12345', NOW()),
            ('$gitlabConnectionId', '$charterId', '$vesselId', 'git', 'gitlab', 'active', '$userId', '67890', NOW());
          """.trimIndent(),
        ).execute()
        .coAwait()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections?provider=github")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val data = json.getJsonArray("data")
        assertEquals(1, data.size())
        assertEquals("github", data.getJsonObject(0).getString("provider"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list with type filter returns only matching connections`(testContext: VertxTestContext) =
    runTest {
      // Create connections with different types
      val gitConnectionId = UUID.randomUUID()
      val registryConnectionId = UUID.randomUUID()

      pg
        .query(
          """
          INSERT INTO oauth_connections (id, charter_id, vessel_id, type, provider, status, created_by_user_id, provider_account_id, created_at)
          VALUES
            ('$gitConnectionId', '$charterId', '$vesselId', 'git', 'github', 'active', '$userId', '12345', NOW()),
            ('$registryConnectionId', '$charterId', '$vesselId', 'registry', 'dockerhub', 'active', '$userId', '67890', NOW());
          """.trimIndent(),
        ).execute()
        .coAwait()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections?type=git")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val data = json.getJsonArray("data")
        assertEquals(1, data.size())
        assertEquals("git", data.getJsonObject(0).getString("type"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list with status filter returns only matching connections`(testContext: VertxTestContext) =
    runTest {
      // Create connections with different statuses
      val activeConnectionId = UUID.randomUUID()
      val pendingConnectionId = UUID.randomUUID()

      pg
        .query(
          """
          INSERT INTO oauth_connections (id, charter_id, vessel_id, type, provider, status, created_by_user_id, provider_account_id, created_at)
          VALUES
            ('$activeConnectionId', '$charterId', '$vesselId', 'git', 'github', 'active', '$userId', '12345', NOW()),
            ('$pendingConnectionId', '$charterId', '$vesselId', 'git', 'gitlab', 'pending', '$userId', '67890', NOW());
          """.trimIndent(),
        ).execute()
        .coAwait()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections?status=active")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val data = json.getJsonArray("data")
        assertEquals(1, data.size())
        assertEquals("active", data.getJsonObject(0).getString("status"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET connections list with wrong Accept header returns 406`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/connections")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/xml")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(406, resp.statusCode())
        testContext.completeNow()
      }
    }
}
