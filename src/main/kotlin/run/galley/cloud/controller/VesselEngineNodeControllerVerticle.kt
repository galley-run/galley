package run.galley.cloud.controller

import generated.jooq.tables.pojos.VesselEngineNodes
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.VesselEngineNodeDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject
import run.galley.cloud.web.JWT
import run.galley.cloud.web.issueGalleyNodeAgentToken

class VesselEngineNodeControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val NODE_AGENT_GET = "nodeAgent.vessel.engine.node.query.get"
    const val LIST = "vessel.engine.node.query.list"
    const val GET = "vessel.engine.node.query.get"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(NODE_AGENT_GET, handler = ::getForNodeAgent)
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
        ?.toJsonAPIResourceObject(
          JsonObject()
            .put(
              "token",
              JWT
                .authProvider(vertx, config)
                .issueGalleyNodeAgentToken(
                  nodeId,
                ),
            ),
        )
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun getForNodeAgent(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val user = apiRequest.user
    val nodeId = user?.subject() ?: throw ApiStatus.VESSEL_ENGINE_NODE_ID_INCORRECT

    val dataRequest =
      EventBusQueryDataRequest(
        identifiers =
          mapOf(
            "id" to nodeId,
          ),
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineNodes>>(VesselEngineNodeDataVerticle.GET, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse, contentType = "application/vnd.galley-node-agent.v1+json"))
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
