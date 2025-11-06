package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselFactory
import run.galley.cloud.sql.VesselEngineSql

class VesselEngineDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.cmd.create"
    const val GET = "data.vessel.engine.query.get"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(GET, handler = ::get)
    }
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.get(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselFactory::from) ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.create(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselFactory::from) ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }
}
