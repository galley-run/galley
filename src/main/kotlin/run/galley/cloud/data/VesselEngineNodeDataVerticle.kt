package run.galley.cloud.data

import generated.jooq.enums.AgentConnectionStatus
import generated.jooq.enums.NodeProvisioningStatus
import generated.jooq.enums.NodeType
import generated.jooq.tables.pojos.VesselEngineNodes
import generated.jooq.tables.pojos.VesselEngines
import generated.jooq.tables.references.VESSEL_ENGINES
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.k8s.parsers.K8sParserNodeList
import run.galley.cloud.model.factory.VesselEngineNodeFactory
import run.galley.cloud.sql.VesselEngineNodeSql
import run.galley.cloud.ws.EventBusAgentResponse
import java.time.OffsetDateTime

class VesselEngineNodeDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.engine.node.cmd.create"
    const val PATCH = "data.vessel.engine.node.cmd.patch"
    const val PATCH_FROM_CLUSTER = "data.vessel.engine.node.cmd.patch_from_cluster"
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
      vertx.eventBus().coConsumer(PATCH_FROM_CLUSTER, handler = ::patchFromCluster)
      vertx.eventBus().coConsumer(LIST_BY_VESSEL_ID, handler = ::listByVesselId)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(SYNC_NODES, handler = ::syncNodes)
      vertx.eventBus().coConsumer(APPLIED, handler = ::applied)
    }
  }

  private suspend fun listByVesselId(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.find(request))

    val vesselEngineNodes = results?.map(VesselEngineNodeFactory::from)

    message.reply(EventBusDataResponse(DataPayload.many(vesselEngineNodes)))
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.get(request))

    val vesselEngineNode =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngineNode)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.create(request))

    val vesselEngineNode =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngineNode)))
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselEngineNodeSql.patch(request))

    val vesselEngineNode =
      results?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngineNode)))
  }

  private suspend fun patchFromCluster(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()

    val findRequest =
      EventBusQueryDataRequest(
        filters = request.filters,
      )

    val result = pool.execute(VesselEngineNodeSql.find(findRequest))

    val updateResult =
      if (result != null && result.size() > 0) {
        pool.execute(VesselEngineNodeSql.patch(request))
      } else {
//        request.payload?.put(VESSEL_ENGINE_NODES.VESSEL_ID.name, vesselId)
        request.payload?.put(VESSEL_ENGINE_NODES.NODE_TYPE.name, NodeType.worker)
        request.payload?.put(VESSEL_ENGINE_NODES.PROVISIONING_STATUS.name, NodeProvisioningStatus.imported)
        pool.execute(VesselEngineNodeSql.create(request))
      }

    val vesselEngineNode =
      updateResult?.firstOrNull()?.let(VesselEngineNodeFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselEngineNode)))
  }

  private suspend fun syncNodes(message: Message<EventBusAgentResponse>) {
    val request = message.body()

    val vesselEngineRequest =
      EventBusCmdDataRequest(
        payload =
          JsonObject()
            .put(VESSEL_ENGINES.AGENT_CONNECTION_STATUS.name, AgentConnectionStatus.connected.toString())
            .put(VESSEL_ENGINES.LAST_AGENT_CONNECTION_AT.name, OffsetDateTime.now().toString()),
        identifier = request.vesselEngineId,
      )

    val vesselEngine =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngines>>(VesselEngineDataVerticle.PATCH, vesselEngineRequest)
        .coAwait()
        .body()
        ?.payload
        ?.toOne() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NOT_FOUND)

    val nodes = K8sParserNodeList(request.payload).nodes

    nodes.forEach { node ->
      val vesselEngineNodeRequest =
        EventBusCmdDataRequest(
          payload =
            JsonObject()
              .put(VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID.name, request.vesselEngineId)
              .put(VESSEL_ENGINE_NODES.VESSEL_ID.name, vesselEngine.vesselId)
              .put(VESSEL_ENGINE_NODES.NAME.name, node.name)
              .put(VESSEL_ENGINE_NODES.IP_ADDRESS.name, node.ip)
              .put(VESSEL_ENGINE_NODES.CPU.name, node.cpu)
              .put(VESSEL_ENGINE_NODES.MEMORY.name, node.memory)
              .put(VESSEL_ENGINE_NODES.STORAGE.name, node.storage)
              .put(VESSEL_ENGINE_NODES.OS_METADATA.name, node.osMetadata.toJsonObject()),
          filters =
            filters {
              VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID eq request.vesselEngineId
              VESSEL_ENGINE_NODES.VESSEL_ID eq vesselEngine.vesselId
              VESSEL_ENGINE_NODES.IP_ADDRESS eq node.ip
            },
        )
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(PATCH_FROM_CLUSTER, vesselEngineNodeRequest)
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
