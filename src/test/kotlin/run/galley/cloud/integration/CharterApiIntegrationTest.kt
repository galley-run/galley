package run.galley.cloud.integration

import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import run.galley.cloud.TestJWTHelper
import run.galley.cloud.crew.CharterCrewAccess
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.model.VesselCrewAccess
import java.util.UUID

class CharterApiIntegrationTest : BaseIntegrationTest() {
  private val vesselId = UUID.randomUUID()
  private val vesselId2 = UUID.randomUUID()
  private val vesselId3 = UUID.randomUUID()
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
            VesselCrewAccess(vesselId, CrewRole.VESSEL_CAPTAIN),
            CharterCrewAccess(vesselId2, charterId, CrewRole.CHARTER_BOATSWAIN),
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
        VALUES ('$userId', 'captain@vessel.com', 'Test', 'Person', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId', 'Test Vessel', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId2', 'Test Vessel 2', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO vessels (id, name, user_id, created_at)
        VALUES ('$vesselId3', 'Test Vessel 3', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charters (id, vessel_id, name, user_id, created_at)
        VALUES ('$charterId', '$vesselId2', 'Test Vessel 1 Charter 1', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        INSERT INTO charters (vessel_id, name, user_id, created_at)
        VALUES ('$vesselId2', 'Test Vessel 1 Charter 2', '$userId', NOW())
        ON CONFLICT (id) DO NOTHING;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  private suspend fun destroyVessel() {
    pg
      .query(
        """
        TRUNCATE TABLE outbox_events, charters, vessels, sessions, users RESTART IDENTITY CASCADE;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

// ==================== POST Tests ====================

  @Test
  fun `test POST charter with valid name and description returns 201`(testContext: VertxTestContext) =
    runTest {
      val charterName = "Test Charter ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName).put("description", "A test charter description")

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val json = resp.bodyAsJsonObject()
      testContext.verify {
        assertEquals(201, resp.statusCode())

        assertNotNull(json.getJsonObject("data").getString("id"))
        assertEquals("Charter", json.getJsonObject("data").getString("type"))
        assertEquals(charterName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertEquals(
          "A test charter description",
          json.getJsonObject("data").getJsonObject("attributes").getString("description"),
        )
        assertEquals(vesselId.toString(), json.getJsonObject("data").getJsonObject("attributes").getString("vesselId"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter with only name (no description) returns 201`(testContext: VertxTestContext) =
    runTest {
      val charterName = "Test Charter No Desc ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(201, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertEquals(charterName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter with null description returns 201`(testContext: VertxTestContext) =
    runTest {
      val charterName = "Charter Null Desc ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName).put("description", null)

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(201, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertEquals(charterName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertNull(json.getJsonObject("data").getJsonObject("attributes").getString("description"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter without name returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("description", "Only description, no name")
      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
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
  fun `test POST charter without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("name", "Unauthored Charter")
      val resp =
        client
          .post("/vessels/$vesselId/charters")
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
  fun `test POST charter with duplicate name returns 409`(testContext: VertxTestContext) =
    runTest {
      val duplicateCharterName = "Duplicate Charter ${UUID.randomUUID()}"
      val body = JsonObject().put("name", duplicateCharterName).put("description", "First charter")

      // First POST - should succeed
      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      assertEquals(201, resp1.statusCode())

      // Second POST with same name - should fail with 409
      val body2 = JsonObject().put("name", duplicateCharterName).put("description", "Duplicate charter")
      val resp2 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body2)
          .coAwait()

      testContext.verify {
        assertEquals(409, resp2.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter with different vessel ID in token returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val body = JsonObject().put("name", "Charter Wrong Vessel")

      val resp =
        client
          .post("/vessels/$differentVesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== GET List Tests ====================

  @Test
  fun `test GET charters list returns 200 with valid token`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter for List ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      client
        .post("/vessels/$vesselId/charters")
        .putHeader("Authorization", "Bearer $validToken")
        .putHeader("Content-Type", "application/vnd.galley.v1+json")
        .putHeader("Accept", "application/vnd.galley.v1+json")
        .sendJsonObject(body)
        .coAwait()

      val resp =
        client
          .get("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        assertEquals(1, json.getJsonArray("data").size())
        assertEquals(
          charterName,
          json
            .getJsonArray("data")
            .getJsonObject(0)
            .getJsonObject("attributes")
            .getString("name"),
        )
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET charters list with access to multiple vessels returns only charters of requested vessel`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val resp2 =
        client
          .get("/vessels/$vesselId2/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val resp3 =
        client
          .get("/vessels/$vesselId3/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()
      val json2 = resp2.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        assertEquals(200, resp2.statusCode())
        assertEquals(403, resp3.statusCode())
        assertEquals(0, json.getJsonArray("data").size())
        assertEquals(1, json2.getJsonArray("data").size())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET charters list without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET charters list with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$differentVesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== GET Single Charter Tests ====================

  @Test
  fun `test GET single charter returns 200 with valid token`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter for Single Get ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Now get the single charter
      val resp2 =
        client
          .get("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(200, resp2.statusCode())
        val json = resp2.bodyAsJsonObject()
        assertEquals(charterId, json.getJsonObject("data").getString("id"))
        assertEquals("Charter", json.getJsonObject("data").getString("type"))
        assertEquals(charterName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertEquals(
          vesselId.toString(),
          json.getJsonObject("data").getJsonObject("attributes").getString("vesselId"),
        )
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET single charter without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val charterId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET non-existent charter returns 404`(testContext: VertxTestContext) =
    runTest {
      val nonExistentCharterId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$nonExistentCharterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(404, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET single charter with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val charterId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$differentVesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Edge Cases ====================

  @Test
  fun `test POST charter with invalid UUID for vessel returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("name", "Test Charter")

      val resp =
        client
          .post("/vessels/not-a-uuid/charters")
          .putHeader("Authorization", "Bearer $validToken")
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
  fun `test GET charter with invalid UUID returns 400`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters/not-a-uuid")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(400, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter with empty name returns 400`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("name", "")

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
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
  fun `test POST charter with extra fields ignores them`(testContext: VertxTestContext) =
    runTest {
      val charterName = "Charter Extra Fields ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", charterName)
          .put("description", "Test")
          .put("extraField", "should be ignored")
          .put("anotherExtra", 123)

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(201, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertEquals(charterName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertNull(json.getJsonObject("data").getJsonObject("attributes").getString("extraField"))
        testContext.completeNow()
      }
    }

  // ==================== PATCH Tests ====================

  @Test
  fun `test PATCH charter with valid data returns 200`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val originalName = "Original Charter ${UUID.randomUUID()}"
      val body = JsonObject().put("name", originalName).put("description", "Original description")

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Now patch it
      val updatedName = "Updated Charter ${UUID.randomUUID()}"
      val patchBody = JsonObject().put("name", updatedName).put("description", "Updated description")

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp2.statusCode())
        val json = resp2.bodyAsJsonObject()
        assertEquals(charterId, json.getJsonObject("data").getString("id"))
        assertEquals(updatedName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertEquals(
          "Updated description",
          json.getJsonObject("data").getJsonObject("attributes").getString("description"),
        )
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH charter with null description clears description`(testContext: VertxTestContext) =
    runTest {
      // Create a charter with description
      val originalName = "Charter With Desc ${UUID.randomUUID()}"
      val body = JsonObject().put("name", originalName).put("description", "Original description")

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Patch with null description should clear it
      val patchBody = JsonObject().put("name", originalName).put("description", null)

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp2.statusCode())
        val json = resp2.bodyAsJsonObject()
        assertEquals(originalName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertNull(json.getJsonObject("data").getJsonObject("attributes").getString("description"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH charter without name returns 400`(testContext: VertxTestContext) =
    runTest {
      val originalName = "Charter to Patch ${UUID.randomUUID()}"
      val body = JsonObject().put("name", originalName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Patch without name should fail
      val patchBody = JsonObject().put("description", "Only description")

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(201, resp1.statusCode())
        assertEquals(400, resp2.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH non-existent charter returns 404`(testContext: VertxTestContext) =
    runTest {
      val nonExistentCharterId = UUID.randomUUID()
      val body = JsonObject().put("name", "Updated Name")

      val resp =
        client
          .patch("/vessels/$vesselId/charters/$nonExistentCharterId")
          .putHeader("Authorization", "Bearer $validToken")
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
  fun `test PATCH charter without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val charterId = UUID.randomUUID()
      val body = JsonObject().put("name", "Updated name")

      val resp =
        client
          .patch("/vessels/$vesselId/charters/$charterId")
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
  fun `test PATCH charter with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val charterId = UUID.randomUUID()
      val body = JsonObject().put("name", "Updated name")

      val resp =
        client
          .patch("/vessels/$differentVesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH charter with duplicate name returns 409`(testContext: VertxTestContext) =
    runTest {
      // Create first charter
      val firstName = "First Charter ${UUID.randomUUID()}"
      val body1 = JsonObject().put("name", firstName)

      client
        .post("/vessels/$vesselId/charters")
        .putHeader("Authorization", "Bearer $validToken")
        .putHeader("Content-Type", "application/vnd.galley.v1+json")
        .putHeader("Accept", "application/vnd.galley.v1+json")
        .sendJsonObject(body1)
        .coAwait()

      // Create second charter
      val secondName = "Second Charter ${UUID.randomUUID()}"
      val body2 = JsonObject().put("name", secondName)

      val resp2 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body2)
          .coAwait()

      val secondCharterId = resp2.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Try to patch second charter with first charter's name - should fail with 409
      val patchBody = JsonObject().put("name", firstName)

      val resp3 =
        client
          .patch("/vessels/$vesselId/charters/$secondCharterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(409, resp3.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== DELETE Tests ====================

  @Test
  fun `test DELETE charter returns 204`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter to Delete ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Delete the charter
      val resp2 =
        client
          .delete("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      assertEquals(204, resp2.statusCode())

      // Verify it's deleted - should return 404
      val resp3 =
        client
          .get("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(404, resp3.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test DELETE non-existent charter returns 404`(testContext: VertxTestContext) =
    runTest {
      val nonExistentCharterId = UUID.randomUUID()

      val resp =
        client
          .delete("/vessels/$vesselId/charters/$nonExistentCharterId")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(404, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test DELETE charter without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val charterId = UUID.randomUUID()

      val resp =
        client.delete("/vessels/$vesselId/charters/$charterId").send().coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test DELETE charter with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val charterId = UUID.randomUUID()

      val resp =
        client
          .delete("/vessels/$differentVesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test DELETE charter with active projects returns 409`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter to Delete ${UUID.randomUUID()}"
      val charterBody = JsonObject().put("name", charterName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(charterBody)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      val projectBody = JsonObject().put("name", "website").put("environment", "production")

      val resp2 =
        client
          .post("/vessels/$vesselId/charters/$charterId/projects")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(projectBody)
          .coAwait()

      // Delete the charter
      val resp3 =
        client
          .delete("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(201, resp1.statusCode())
        assertEquals(201, resp2.statusCode())
        assertEquals(409, resp3.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Content-Type and Accept Header Tests ====================

  @Test
  fun `test POST charter with wrong Content-Type returns 415`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("name", "Test Charter")

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/json") // Wrong content type
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(415, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter with wrong Accept header returns 406`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("name", "Test Charter")

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/xml") // Wrong accept header
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(406, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST charter with text-plain Content-Type returns 415`(testContext: VertxTestContext) =
    runTest {
      val body = JsonObject().put("name", "Test Charter")

      val resp =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "text/plain")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(415, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH charter with wrong Content-Type returns 415`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter for Content Type Test ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Try to patch with wrong content type
      val patchBody = JsonObject().put("name", "Updated Name")

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/json") // Wrong content type
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(415, resp2.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH charter with wrong Accept header returns 406`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter for Accept Test ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Try to patch with wrong accept header
      val patchBody = JsonObject().put("name", "Updated Name")

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "text/html") // Wrong accept header
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(406, resp2.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET charter with wrong Accept header returns 406`(testContext: VertxTestContext) =
    runTest {
      // Create a charter first
      val charterName = "Charter for GET Accept Test ${UUID.randomUUID()}"
      val body = JsonObject().put("name", charterName)

      val resp1 =
        client
          .post("/vessels/$vesselId/charters")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val charterId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Try to GET with wrong accept header
      val resp2 =
        client
          .get("/vessels/$vesselId/charters/$charterId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/xml") // Wrong accept header
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(406, resp2.statusCode())
        testContext.completeNow()
      }
    }
}
