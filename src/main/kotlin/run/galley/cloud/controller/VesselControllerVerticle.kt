package run.galley.cloud.controller

import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.util.UUID
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.model.Vessel

class VesselControllerVerticle : CoroutineVerticle() {
  companion object {
    const val ADDRESS_LIST = "vessel.query.list"
    const val ADDRESS_GET = "vessel.query.get"
    const val ADDRESS_CREATE = "vessel.cmd.create"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST, ::list)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET, ::get)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_CREATE, ::create)
  }

  private suspend fun list(message: Message<EventBusDataRequest>) {
    if (message.body().version == "v1") {
      message.reply(
        EventBusDataResponse(
          payload = DataPayload.many(
            Vessel(UUID.randomUUID(), "Henk"),
            Vessel(name = "Bert", desk = "Kantoor")
          ),
          version = "v1",
        )
      )
    }
  }

  private suspend fun get(message: Message<EventBusDataRequest>) {
    if (message.body().version == "v1") {
      message.reply(
        EventBusDataResponse(
          payload = DataPayload.one(
            Vessel(UUID.randomUUID(), "Henk")
          )
        )
      )
    }
  }

  private suspend fun create(message: Message<EventBusDataRequest>) {
    throw ApiStatusReplyException(ApiStatus.FAILED_INSERT)
  }
}
