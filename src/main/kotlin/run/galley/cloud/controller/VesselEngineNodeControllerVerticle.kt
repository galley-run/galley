package run.galley.cloud.controller

import generated.jooq.enums.EngineMode
import generated.jooq.enums.NodeProvisioningStatus
import generated.jooq.tables.pojos.VesselEngineNodes
import generated.jooq.tables.pojos.VesselEngines
import generated.jooq.tables.references.VESSEL_ENGINES
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.VesselEngineDataVerticle
import run.galley.cloud.data.VesselEngineNodeDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject
import run.galley.cloud.web.JWT
import run.galley.cloud.web.issueGalleyNodeAgentToken

class VesselEngineNodeControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val NODE_AGENT_GET = "nodeAgent.vessel.engine.node.query.get"
    const val NODE_AGENT_PATCH = "nodeAgent.vessel.engine.node.query.patch"
    const val LIST = "vessel.engine.node.query.list"
    const val GET = "vessel.engine.node.query.get"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(NODE_AGENT_GET, handler = ::getForNodeAgent)
      vertx.eventBus().coConsumer(NODE_AGENT_PATCH, handler = ::patchByNodeAgent)
    }
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val vesselId = apiRequest.vesselId
    val nodeId = apiRequest.nodeId

    val dataRequest =
      EventBusQueryDataRequest(
        identifiers =
          mapOf(
            "id" to nodeId.toString(),
          ),
        filters =
          filters {
            VESSEL_ENGINE_NODES.VESSEL_ID eq vesselId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.GET, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    val token =
      if (dataResponse.provisioningStatus === NodeProvisioningStatus.open) {
        JWT
          .authProvider(vertx, config)
          .issueGalleyNodeAgentToken(
            nodeId,
          )
      } else {
        null
      }
    val obj =
      dataResponse.toJsonAPIResourceObject(
        JsonObject()
          .put("token", token),
      )

    message.reply(EventBusApiResponse(obj))
  }

  private suspend fun getForNodeAgent(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val nodeId = apiRequest.user?.subject() ?: throw ApiStatus.VESSEL_ENGINE_NODE_ID_INCORRECT

    val dataRequest =
      EventBusQueryDataRequest(
        identifiers =
          mapOf(
            "id" to nodeId,
          ),
      )

    val vesselEngineNode =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.GET, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    // Only allow this endpoint when the provisioning status is open
    if (vesselEngineNode.provisioningStatus !== NodeProvisioningStatus.open) {
      throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)
    }

    message.reply(
      EventBusApiResponse(
        vesselEngineNode.toJsonAPIResourceObject(),
        contentType = "application/vnd.galley-node-agent.v1+json",
      ),
    )
  }

  private suspend fun patchByNodeAgent(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val nodeId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatus.VESSEL_ENGINE_NODE_ID_INCORRECT

    val dataRequest =
      EventBusCmdDataRequest(
        payload = apiRequest.body,
        identifier = nodeId,
        filters =
          filters {
            VESSEL_ENGINE_NODES.PROVISIONING_STATUS eq NodeProvisioningStatus.open
          },
      )

    val vesselEngineNode =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    val vesselEngineDataRequest =
      EventBusCmdDataRequest(
        payload = JsonObject().put(VESSEL_ENGINES.MODE.name, EngineMode.managed_cloud.toString()),
        identifier = vesselEngineNode.vesselEngineId,
      )

    vertx
      .eventBus()
      .request<EventBusDataResponse<VesselEngines>>(
        VesselEngineDataVerticle.PATCH,
        vesselEngineDataRequest,
      ).coAwait()

    message.reply(
      EventBusApiResponse(
        vesselEngineNode.toJsonAPIResourceObject(),
        contentType = "application/vnd.galley-node-agent.v1+json",
      ),
    )
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId

    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            VESSEL_ENGINE_NODES.VESSEL_ID eq vesselId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.LIST_BY_VESSEL_ID, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(dataResponse))
  }
}
