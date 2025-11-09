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
import run.galley.cloud.model.factory.VesselEngineNodeFactory
import run.galley.cloud.sql.VesselEngineNodeSql
import run.galley.cloud.ws.EventBusAgentRequest

class VesselEngineNodeDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.node.cmd.create"
    const val LIST_BY_VESSEL_ID = "data.vessel.engine.node.query.list_by_vessel_id"
    const val SYNC_NODES = "data.vessel.engine.node.cmd.sync_nodes"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(LIST_BY_VESSEL_ID, handler = ::listByVesselId)
      vertx.eventBus().coConsumer(SYNC_NODES, handler = ::syncNodes)
    }
  }

  private suspend fun listByVesselId(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.getByVesselId(request))

    val vesselEngines = results?.map(VesselEngineNodeFactory::from)

    message.reply(EventBusDataResponse(DataPayload.many(vesselEngines)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.create(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun syncNodes(message: Message<EventBusAgentRequest>) {
    val request = message.body()
    println(request.toSocketMessage())
  }
}
