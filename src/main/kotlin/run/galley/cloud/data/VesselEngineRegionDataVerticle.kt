package run.galley.cloud.data

import generated.jooq.tables.pojos.CharterProjects
import generated.jooq.tables.pojos.VesselEngineRegions
import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselEngineRegionFactory
import run.galley.cloud.sql.VesselEngineRegionSql

class VesselEngineRegionDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.region.cmd.create"
    const val LIST_BY_VESSEL_ID = "data.vessel.engine.region.query.list_by_vessel_id"
    const val GET = "data.vessel.engine.region.query.get"
    const val PATCH = "data.vessel.engine.region.cmd.patch"
    const val DELETE = "data.vessel.engine.region.cmd.delete"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(LIST_BY_VESSEL_ID, handler = ::listByVesselId)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(DELETE, handler = ::delete)
    }
  }

  private suspend fun listByVesselId(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineRegionSql.getByVesselId(request))

    val regions = results?.map(VesselEngineRegionFactory::from)

    message.reply(EventBusDataResponse(DataPayload.many(regions)))
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineRegionSql.get(request))

    val region =
      results?.firstOrNull()?.let(VesselEngineRegionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_REGION_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(region)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineRegionSql.create(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineRegionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineRegionSql.patch(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineRegionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun delete(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val updated = pool.execute(VesselEngineRegionSql.delete(request))

    if (updated?.rowCount() == 0) {
      throw ApiStatusReplyException(ApiStatus.VESSEL_REGION_NOT_FOUND)
    }

    message.reply(EventBusDataResponse.noContent<VesselEngineRegions>())
  }
}
