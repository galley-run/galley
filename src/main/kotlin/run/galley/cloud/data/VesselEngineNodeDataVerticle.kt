package run.galley.cloud.data

import generated.jooq.enums.AgentConnectionStatus
import generated.jooq.tables.VesselEngines
import generated.jooq.tables.references.VESSEL_ENGINES
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.k8s.parsers.K8sParserNodeList
import run.galley.cloud.model.factory.VesselEngineNodeFactory
import run.galley.cloud.sql.VesselEngineNodeSql
import run.galley.cloud.ws.EventBusAgentResponse

class VesselEngineNodeDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.node.cmd.create"
    const val PATCH = "data.vessel.engine.node.cmd.patch"
    const val GET = "data.vessel.engine.node.query.get"
    const val LIST_BY_VESSEL_ID = "data.vessel.engine.node.query.list_by_vessel_id"
    const val SYNC_NODES = "data.vessel.engine.node.cmd.sync_nodes"
    const val APPLIED = "data.vessel.engine.node.cmd.applied"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(LIST_BY_VESSEL_ID, handler = ::listByVesselId)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(SYNC_NODES, handler = ::syncNodes)
      vertx.eventBus().coConsumer(APPLIED, handler = ::applied)
    }
  }

  private suspend fun listByVesselId(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.getByVesselId(request))

    val vesselEngines = results?.map(VesselEngineNodeFactory::from)

    message.reply(EventBusDataResponse(DataPayload.many(vesselEngines)))
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.get(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.create(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.patch(request))

    val vesselEngine =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngine)))
  }

  private suspend fun syncNodes(message: Message<EventBusAgentResponse>) {
    val request = message.body()

    val vesselEngineRequest =
      EventBusCmdDataRequest(
        payload =
          JsonObject()
            .put(VESSEL_ENGINES.AGENT_CONNECTION_STATUS.name, AgentConnectionStatus.connected),
        identifier = request.vesselEngineId,
      )
    vertx
      .eventBus()
      .request<EventBusDataResponse<VesselEngines>>(VesselEngineDataVerticle.PATCH, vesselEngineRequest)
      .coAwait()

    val nodes = K8sParserNodeList(request.payload).nodes

    nodes.forEach { node ->
      val vesselEngineRequest =
        EventBusCmdDataRequest(
          payload =
            JsonObject()
              .put(VESSEL_ENGINES.AGENT_CONNECTION_STATUS.name, AgentConnectionStatus.connected),
          identifier = request.vesselEngineId,
        )
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngines>>(PATCH, vesselEngineRequest)
        .coAwait()
    }

    println(nodes)
  }

  private suspend fun applied(message: Message<EventBusAgentResponse>) {
    val request = message.body()

    println("===========================================================")
    println("APPLIED:")
    println(request.payload)
    println("===========================================================")
  }
}
