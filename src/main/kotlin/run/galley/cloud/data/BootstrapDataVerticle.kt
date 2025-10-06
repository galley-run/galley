package run.galley.cloud.data

import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait

class BootstrapDataVerticle : CoroutineVerticle() {
  override suspend fun start() {
    super.start()

    vertx.deployVerticle(VesselDataVerticle(), deploymentOptionsOf(config)).coAwait()
  }
}
