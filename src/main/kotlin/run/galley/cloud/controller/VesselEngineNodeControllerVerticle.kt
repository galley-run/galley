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
import nl.clicqo.web.HttpStatus
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
    const val CREATE = "vessel.engine.node.cmd.create"
    const val PATCH = "vessel.engine.node.cmd.patch"
    const val DELETE = "vessel.engine.node.cmd.delete"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(NODE_AGENT_GET, handler = ::getForNodeAgent)
      vertx.eventBus().coConsumer(NODE_AGENT_PATCH, handler = ::patchByNodeAgent)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(DELETE, handler = ::delete)
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
    val nodeId = apiRequest.user?.subject() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_ID_INCORRECT)

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
    val nodeId =
      apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_ID_INCORRECT)

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

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val vesselId = apiRequest.vesselId

    val vesselEngine =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngines>>(
          VesselEngineDataVerticle.LIST_BY_VESSEL_ID,
          EventBusQueryDataRequest(
            filters =
              filters {
                VESSEL_ENGINES.VESSEL_ID eq vesselId
              },
          ),
        ).coAwait()
        .body()
        ?.payload
        ?.toMany()
        ?.firstOrNull()

    val vesselEngineId = vesselEngine?.id ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_ID_INCORRECT)
    val node = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)
    node.put(VESSEL_ENGINE_NODES.VESSEL_ID.name, vesselId.toString())
    node.put(VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID.name, vesselEngineId.toString())

    val dataRequest =
      EventBusCmdDataRequest(
        payload = node,
      )

    val response =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    message.reply(
      EventBusApiResponse(
        response,
        httpStatus = HttpStatus.Created,
      ),
    )
  }

  private suspend fun patch(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val vesselId = apiRequest.vesselId
    val nodeId = apiRequest.nodeId

    val node = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = node,
        identifier = nodeId,
        filters =
          filters {
            VESSEL_ENGINE_NODES.VESSEL_ID eq vesselId
          },
      )

    val response =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    message.reply(EventBusApiResponse(response))
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

  private suspend fun delete(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val nodeId = apiRequest.nodeId

    // TODO: Currently you can only delete nodes with provisioning_status set to Open. we may want to change that in the
    //  future, but then we need to check other prerequisites before we may delete this node.

    val deleteRequest =
      EventBusCmdDataRequest(
        identifier = nodeId,
        filters =
          filters {
            VESSEL_ENGINE_NODES.VESSEL_ID eq vesselId
            VESSEL_ENGINE_NODES.PROVISIONING_STATUS eq NodeProvisioningStatus.open
          },
      )

    vertx
      .eventBus()
      .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.DELETE, deleteRequest)
      .coAwait()

    message.reply(EventBusApiResponse())
  }
}
