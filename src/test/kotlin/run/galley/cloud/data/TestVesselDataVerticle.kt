package run.galley.cloud.data

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import run.galley.cloud.TestVerticle

@ExtendWith(VertxExtension::class)
class TestVesselDataVerticle: TestVerticle() {
  @BeforeEach
  fun deploy_verticle(vertx: Vertx, testContext: VertxTestContext) {
    vertx.deployVerticle(VesselDataVerticle())
      .onComplete(testContext.succeeding<String> { _ -> testContext.completeNow() })
  }

  @Test
  fun verticle_deployed(vertx: Vertx, testContext: VertxTestContext) {
    testContext.completeNow()
  }

  @Test
  fun test_list_function(vertx: Vertx, testContext: VertxTestContext) {
    vertx.eventBus().request<EventBusApiResponse>(
      VesselDataVerticle.ADDRESS_LIST,
      EventBusApiRequest()
    ).onComplete(testContext.succeeding { response ->
      val body = response.body()
      assertNotNull(body)
      assertInstanceOf(EventBusApiResponse::class.java, body)

      testContext.completeNow()
    })
  }
}
