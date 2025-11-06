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
import run.galley.cloud.model.factory.VesselEngineFactory
import run.galley.cloud.sql.VesselEngineSql

class VesselEngineDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.cmd.create"
    const val LIST_BY_VESSEL_ID = "data.vessel.engine.query.list_by_vessel_id"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(LIST_BY_VESSEL_ID, handler = ::listByVesselId)
    }
  }

  private suspend fun listByVesselId(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.getByVesselId(request))

    val vesselEngines = results?.map(VesselEngineFactory::from)

    message.reply(EventBusDataResponse(DataPayload.many(vesselEngines)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineSql.create(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }
}
