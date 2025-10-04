package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.util.UUID
import nl.clicqo.data.DataPayload
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.model.Vessel

class VesselDataVerticle : CoroutineVerticle() {
  companion object {
    const val ADDRESS_LIST = "data.vessel.query.list"
    const val ADDRESS_GET = "data.vessel.query.get"
    const val ADDRESS_CREATE = "data.vessel.cmd.create"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST, ::list)
  }

  private suspend fun list(message: Message<EventBusDataRequest>) {
    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(
          Vessel(UUID.randomUUID(), "Henk"),
          Vessel(UUID.randomUUID(), "Bart"),
          Vessel(UUID.randomUUID(), "Willem"),
        )
      )
    )
  }
}
