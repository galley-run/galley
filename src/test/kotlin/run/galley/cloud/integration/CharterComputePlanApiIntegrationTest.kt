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
import java.util.UUID

class CharterComputePlanApiIntegrationTest : BaseIntegrationTest() {
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
            CharterCrewAccess(vesselId, charterId, CrewRole.CHARTER_PURSER),
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
        VALUES ('$userId', 'user@vessel.com', 'Test', 'User', NOW())
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
        TRUNCATE TABLE charter_compute_plans, charter_projects, outbox_events, charters, crew, vessels, sessions, users RESTART IDENTITY CASCADE;
        """.trimIndent(),
      ).execute()
      .coAwait()
  }

  // ==================== POST Tests ====================

  @Test
  fun `test POST compute plan with valid data returns 200`(testContext: VertxTestContext) =
    runTest {
      val planName = "Production Plan ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "2")
              .put("memory", "4G"),
          ).put(
            "limits",
            JsonObject()
              .put("cpu", "4")
              .put("memory", "8G"),
          ).put(
            "billing",
            JsonObject()
              .put("enabled", true)
              .put("period", "monthly")
              .put("unitPrice", "99.99"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val json = resp.bodyAsJsonObject()
      testContext.verify {
        assertEquals(200, resp.statusCode())
        assertNotNull(json.getJsonObject("data").getString("id"))
        assertEquals("CharterComputePlan", json.getJsonObject("data").getString("type"))
        assertEquals(planName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertEquals(
          "applications",
          json.getJsonObject("data").getJsonObject("attributes").getString("application"),
        )

        val requests = json.getJsonObject("data").getJsonObject("attributes").getJsonObject("requests")
        assertEquals("2", requests.getString("cpu"))
        assertEquals("4G", requests.getString("memory"))

        val limits = json.getJsonObject("data").getJsonObject("attributes").getJsonObject("limits")
        assertEquals("4", limits.getString("cpu"))
        assertEquals("8G", limits.getString("memory"))

        val billing = json.getJsonObject("data").getJsonObject("attributes").getJsonObject("billing")
        assertEquals(true, billing.getBoolean("enabled"))
        assertEquals("monthly", billing.getString("period"))
        assertEquals("99.99", billing.getString("unitPrice"))

        testContext.completeNow()
      }
    }

  @Test
  fun `test POST compute plan with minimal data returns 200`(testContext: VertxTestContext) =
    runTest {
      val planName = "Minimal Plan ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", "databases")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertEquals(planName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test POST compute plan without name returns 400`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
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
  fun `test POST compute plan without application returns 400`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("name", "Plan Without App")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
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
  fun `test POST compute plan without requests returns 400`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("name", "Plan Without Requests")
          .put("application", "applications")

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
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
  fun `test POST compute plan without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("name", "Unauthorized Plan")
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
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
  fun `test POST compute plan with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val body =
        JsonObject()
          .put("name", "Plan Wrong Vessel")
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$differentVesselId/charters/$charterId/compute-plans")
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
  fun `test GET compute plans list returns 200 with valid token`(testContext: VertxTestContext) =
    runTest {
      // Create a compute plan first
      val planName = "Plan for List ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      client
        .post("/vessels/$vesselId/charters/$charterId/compute-plans")
        .putHeader("Authorization", "Bearer $validToken")
        .putHeader("Content-Type", "application/vnd.galley.v1+json")
        .putHeader("Accept", "application/vnd.galley.v1+json")
        .sendJsonObject(body)
        .coAwait()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      val json = resp.bodyAsJsonObject()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        assertEquals(1, json.getJsonArray("data").size())
        assertEquals(
          planName,
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
  fun `test GET compute plans list without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET compute plans list with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$differentVesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== GET Single Compute Plan Tests ====================

  @Test
  fun `test GET single compute plan returns 200 with valid token`(testContext: VertxTestContext) =
    runTest {
      // Create a compute plan first
      val planName = "Plan for Single Get ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", "applications_databases")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "2")
              .put("memory", "2G"),
          ).put(
            "billing",
            JsonObject()
              .put("enabled", true)
              .put("period", "monthly")
              .put("unitPrice", "49.99"),
          )

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val computePlanId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Now get the single compute plan
      val resp2 =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(200, resp2.statusCode())
        val json = resp2.bodyAsJsonObject()
        assertEquals(computePlanId, json.getJsonObject("data").getString("id"))
        assertEquals("CharterComputePlan", json.getJsonObject("data").getString("type"))
        assertEquals(planName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertEquals(
          "applications_databases",
          json.getJsonObject("data").getJsonObject("attributes").getString("application"),
        )
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET single compute plan without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val computePlanId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test GET non-existent compute plan returns 404`(testContext: VertxTestContext) =
    runTest {
      val nonExistentComputePlanId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans/$nonExistentComputePlanId")
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
  fun `test GET single compute plan with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val computePlanId = UUID.randomUUID()

      val resp =
        client
          .get("/vessels/$differentVesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== PATCH Tests ====================

  @Test
  fun `test PATCH compute plan with valid data returns 200`(testContext: VertxTestContext) =
    runTest {
      // Create a compute plan first
      val originalName = "Original Plan ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", originalName)
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val computePlanId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Now patch it
      val updatedName = "Updated Plan ${UUID.randomUUID()}"
      val patchBody =
        JsonObject()
          .put("name", updatedName)
          .put(
            "requests",
            JsonObject()
              .put("cpu", "4")
              .put("memory", "8G"),
          ).put(
            "limits",
            JsonObject()
              .put("cpu", "8")
              .put("memory", "16G"),
          )

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp2.statusCode())
        val json = resp2.bodyAsJsonObject()
        assertEquals(computePlanId, json.getJsonObject("data").getString("id"))
        assertEquals(updatedName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))

        val requests = json.getJsonObject("data").getJsonObject("attributes").getJsonObject("requests")
        assertEquals("4", requests.getString("cpu"))
        assertEquals("8G", requests.getString("memory"))

        val limits = json.getJsonObject("data").getJsonObject("attributes").getJsonObject("limits")
        assertEquals("8", limits.getString("cpu"))
        assertEquals("16G", limits.getString("memory"))

        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH compute plan with partial update returns 200`(testContext: VertxTestContext) =
    runTest {
      // Create a compute plan
      val originalName = "Plan With Billing ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", originalName)
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          ).put(
            "billing",
            JsonObject()
              .put("enabled", true)
              .put("period", "monthly")
              .put("unitPrice", "10.00"),
          )

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val computePlanId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Patch only billing
      val patchBody =
        JsonObject()
          .put(
            "billing",
            JsonObject()
              .put("enabled", false),
          )

      val resp2 =
        client
          .patch("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(patchBody)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp2.statusCode())
        val json = resp2.bodyAsJsonObject()
        val billing = json.getJsonObject("data").getJsonObject("attributes").getJsonObject("billing")
        assertEquals(false, billing.getBoolean("enabled"))
        testContext.completeNow()
      }
    }

  @Test
  fun `test PATCH non-existent compute plan returns 404`(testContext: VertxTestContext) =
    runTest {
      val nonExistentComputePlanId = UUID.randomUUID()
      val body = JsonObject().put("name", "Updated Name")

      val resp =
        client
          .patch("/vessels/$vesselId/charters/$charterId/compute-plans/$nonExistentComputePlanId")
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
  fun `test PATCH compute plan without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val computePlanId = UUID.randomUUID()
      val body = JsonObject().put("name", "Updated name")

      val resp =
        client
          .patch("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
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
  fun `test PATCH compute plan with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val computePlanId = UUID.randomUUID()
      val body = JsonObject().put("name", "Updated name")

      val resp =
        client
          .patch("/vessels/$differentVesselId/charters/$charterId/compute-plans/$computePlanId")
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

  // ==================== DELETE Tests ====================

  @Test
  fun `test DELETE compute plan returns 204`(testContext: VertxTestContext) =
    runTest {
      // Create a compute plan first
      val planName = "Plan to Delete ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val computePlanId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Delete the compute plan
      val resp2 =
        client
          .delete("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      assertEquals(204, resp2.statusCode())

      // Verify it's deleted - should return 404
      val resp3 =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
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
  fun `test DELETE non-existent compute plan returns 404`(testContext: VertxTestContext) =
    runTest {
      val nonExistentComputePlanId = UUID.randomUUID()

      val resp =
        client
          .delete("/vessels/$vesselId/charters/$charterId/compute-plans/$nonExistentComputePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(404, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test DELETE compute plan without authentication returns 401`(testContext: VertxTestContext) =
    runTest {
      val computePlanId = UUID.randomUUID()

      val resp =
        client
          .delete("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(401, resp.statusCode())
        testContext.completeNow()
      }
    }

  @Test
  fun `test DELETE compute plan with different vessel ID returns 403`(testContext: VertxTestContext) =
    runTest {
      val differentVesselId = UUID.randomUUID()
      val computePlanId = UUID.randomUUID()

      val resp =
        client
          .delete("/vessels/$differentVesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(403, resp.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Content-Type and Accept Header Tests ====================

  @Test
  fun `test POST compute plan with wrong Content-Type returns 415`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("name", "Test Plan")
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
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
  fun `test POST compute plan with wrong Accept header returns 406`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("name", "Test Plan")
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
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
  fun `test GET compute plan with wrong Accept header returns 406`(testContext: VertxTestContext) =
    runTest {
      // Create a compute plan first
      val planName = "Plan for Accept Test ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp1 =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      val computePlanId = resp1.bodyAsJsonObject().getJsonObject("data").getString("id")

      // Try to GET with wrong accept header
      val resp2 =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans/$computePlanId")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Accept", "application/xml") // Wrong accept header
          .send()
          .coAwait()

      testContext.verify {
        assertEquals(406, resp2.statusCode())
        testContext.completeNow()
      }
    }

  // ==================== Edge Cases ====================

  @Test
  fun `test POST compute plan with invalid UUID for vessel returns 400`(testContext: VertxTestContext) =
    runTest {
      val body =
        JsonObject()
          .put("name", "Test Plan")
          .put("application", "applications")
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/not-a-uuid/charters/$charterId/compute-plans")
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
  fun `test GET compute plan with invalid UUID returns 400`(testContext: VertxTestContext) =
    runTest {
      val resp =
        client
          .get("/vessels/$vesselId/charters/$charterId/compute-plans/not-a-uuid")
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
  fun `test POST compute plan with null application returns 200`(testContext: VertxTestContext) =
    runTest {
      val planName = "Plan Null App ${UUID.randomUUID()}"
      val body =
        JsonObject()
          .put("name", planName)
          .put("application", null)
          .put(
            "requests",
            JsonObject()
              .put("cpu", "1")
              .put("memory", "1G"),
          )

      val resp =
        client
          .post("/vessels/$vesselId/charters/$charterId/compute-plans")
          .putHeader("Authorization", "Bearer $validToken")
          .putHeader("Content-Type", "application/vnd.galley.v1+json")
          .putHeader("Accept", "application/vnd.galley.v1+json")
          .sendJsonObject(body)
          .coAwait()

      testContext.verify {
        assertEquals(200, resp.statusCode())
        val json = resp.bodyAsJsonObject()
        assertEquals(planName, json.getJsonObject("data").getJsonObject("attributes").getString("name"))
        assertNull(json.getJsonObject("data").getJsonObject("attributes").getString("application"))
        testContext.completeNow()
      }
    }
}
