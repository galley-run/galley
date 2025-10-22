package run.galley.cloud.integration

import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import run.galley.cloud.TestJWTHelper
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.model.VesselCrewAccess
import java.util.UUID

class ProjectIntegrationTest : BaseIntegrationTest() {
  private val vesselId = UUID.randomUUID()
  private val vesselId2 = UUID.randomUUID()
  private val charterId = UUID.randomUUID()
  private val charterId2 = UUID.randomUUID()
  private val projectId1 = UUID.randomUUID()
  private val projectId2 = UUID.randomUUID()
  private val userIdVC = UUID.randomUUID()
  private val userIdCC = UUID.randomUUID()
  private val userIdCB = UUID.randomUUID()
  private val userIdCP = UUID.randomUUID()
  private lateinit var vesselCaptain: String
  private lateinit var charterBoatswain: String
  private lateinit var charterPurser: String

  @BeforeEach
  override fun setupEach() {
    super.setupEach()

    vesselCaptain =
      TestJWTHelper.generateAccessToken(
        getJWTAuth(),
        userId = userIdVC,
        crewAccess =
          listOf(
            VesselCrewAccess(vesselId, CrewRole.VESSEL_CAPTAIN),
            CharterCrewAccess(vesselId2, charterId2, CrewRole.CHARTER_CAPTAIN),
          ),
      )
    charterBoatswain =
      TestJWTHelper.generateAccessToken(
        getJWTAuth(),
        userId = userIdCB,
        crewAccess =
          listOf(
            CharterCrewAccess(vesselId2, charterId2, CrewRole.CHARTER_BOATSWAIN),
          ),
      )
    charterPurser =
      TestJWTHelper.generateAccessToken(
        getJWTAuth(),
        userId = userIdCP,
        crewAccess =
          listOf(
            CharterCrewAccess(vesselId, charterId, CrewRole.CHARTER_PURSER),
          ),
      )

    // Clean database and preload required data
    runTest {
      destroyVessel()
      preloadVessel()
    }
  }

  @AfterEach
  fun teardownEach() {
    runTest { destroyVessel() }
    client.close()
  }

  private suspend fun preloadVessel() {
    pg
      .query(
        """
        INSERT INTO users (id, email, first_name, last_name, created_at)
        VALUES ('$userIdVC', 'captain@vessel.com', 'Test', 'Person', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId', 'Test Vessel', '$userIdVC', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId2', 'Test Vessel 2', '$userIdVC', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charters (id, vessel_id, name, user_id, created_at)
        VALUES ('$charterId', '$vesselId', 'Test Vessel 1 Charter 1', '$userIdVC', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charters (id, vessel_id, name, user_id, created_at)
        VALUES ('$charterId2', '$vesselId2', 'Test Vessel 2 Charter 2', '$userIdVC', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charter_projects (id, vessel_id, charter_id, name, environment, purpose)
        VALUES ('$projectId1', '$vesselId', '$charterId', 'Test Vessel 1 Charter 1 Project 1', 'production', 'website')
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charter_projects (id, vessel_id, charter_id, name, environment, purpose)
        VALUES ('$projectId2', '$vesselId2', '$charterId2', 'Test Vessel 2 Charter 2 Project 2', 'staging', 'api')
        ON CONFLICT (id) DO NOTHING;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  private suspend fun destroyVessel() {
    pg
      .query(
        """
        TRUNCATE TABLE outbox_events, charters, vessels, charter_projects, sessions, users RESTART IDENTITY CASCADE;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  @Test
  fun `test GET Projects with various crew roles returns correctly`(testContext: VertxTestContext) =
    runTest {
      // User is not authorized
      val resp1 =
        client
          .get("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()
      // User is Vessel captain of vessel 1
      val resp2 =
        client
          .get("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()
      // User is Charter captain of charter 1 of Vessel 2, so this combo should fail, since we request charter 1 of vessel 2
      val resp3 =
        client
          .get("/vessels/$vesselId2/charters/$charterId/projects")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()
      // User is Charter captain of charter 2 of Vessel 2
      val resp4 =
        client
          .get("/vessels/$vesselId2/charters/$charterId2/projects")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp1.statusCode())
        assertEquals(200, resp2.statusCode())
        assertEquals(1, resp2.bodyAsJsonObject().getJsonArray("data").size())
        assertEquals(403, resp3.statusCode())
        assertEquals(200, resp4.statusCode())
        assertEquals(1, resp4.bodyAsJsonObject().getJsonArray("data").size())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST Project only allows when you're authorized for the charter`(testContext: VertxTestContext) =
    runTest {
      val project1 =
        JsonObject()
          .put("name", "website.com")
          .put("environment", "production")
      val project2 =
        JsonObject()
          .put("name", "website.com")
          .put("environment", "staging")

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .sendJsonObject(project1)
          .coAwait()
      val resp2 =
        client
          .post("/vessels/$vesselId2/charters/$charterId2/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $charterBoatswain")
          .sendJsonObject(project1)
          .coAwait()
      val resp3 =
        client
          .post("/vessels/$vesselId2/charters/$charterId2/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .sendJsonObject(project2)
          .coAwait()
      testContext.verify {
        assertEquals(201, resp1.statusCode())
        assertEquals(
          project1.getString("name"),
          resp1
            .bodyAsJsonObject()
            .getJsonObject("data")
            .getJsonObject("attributes")
            .getString("name"),
        )
        assertEquals(201, resp2.statusCode())
        assertEquals(201, resp3.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST fails when you're not authorized for the charter`(testContext: VertxTestContext) =
    runTest {
      val project1 =
        JsonObject()
          .put("name", "website.com")
          .put("environment", "production")

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $charterPurser")
          .sendJsonObject(project1)
          .coAwait()
      val resp2 =
        client
          .post("/vessels/$vesselId2/charters/$charterId2/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $charterPurser")
          .sendJsonObject(project1)
          .coAwait()
      testContext.verify {
        assertEquals(403, resp1.statusCode())
        assertEquals(403, resp2.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST Project fails when name or environment is missing`(testContext: VertxTestContext) =
    runTest {
      val project1 =
        JsonObject()
          .put("name", "website.com")
          .put("purpose", "website")

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .sendJsonObject(project1)
          .coAwait()

      val json1 =
        resp1
          .bodyAsJsonObject()
          .getJsonArray("errors")

      val project2 =
        JsonObject()
          .put("environment", "production")
          .put("purpose", "website")

      val resp2 =
        client
          .post("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $vesselCaptain")
          .sendJsonObject(project2)
          .coAwait()

      val json2 =
        resp2
          .bodyAsJsonObject()
          .getJsonArray("errors")

      testContext.verify {
        assertEquals(400, resp1.statusCode())
        assertTrue(json1.getJsonObject(0).getString("title").contains("\"environment\""))
        assertEquals(400, resp2.statusCode())
        assertTrue(json2.getJsonObject(0).getString("title").contains("\"name\""))
        testContext.completeNow()
      }
    }
}
