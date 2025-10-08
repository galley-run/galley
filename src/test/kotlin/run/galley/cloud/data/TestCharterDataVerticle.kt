package run.galley.cloud.data

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.deploymentOptionsOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import run.galley.cloud.TestVerticle

@ExtendWith(VertxExtension::class)
class TestCharterDataVerticle : TestVerticle() {
  @BeforeEach
  fun deploy_verticle(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    vertx
      .deployVerticle(CharterDataVerticle(), deploymentOptionsOf(config))
      .onComplete(testContext.succeeding { _ -> testContext.completeNow() })
  }

  @Test
  fun verticle_deployed(
    vertx: Vertx,
    testContext: VertxTestContext,
  ) {
    testContext.completeNow()
  }
}
