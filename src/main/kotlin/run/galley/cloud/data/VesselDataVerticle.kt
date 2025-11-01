package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselFactory
import run.galley.cloud.sql.VesselSql

class VesselDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
    }
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselSql.create(request))

    val vessel =
      results?.firstOrNull()?.let(VesselFactory::from) ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vessel)))
  }
}
