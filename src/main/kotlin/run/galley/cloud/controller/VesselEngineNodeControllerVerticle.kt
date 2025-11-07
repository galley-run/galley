package run.galley.cloud.controller

import generated.jooq.tables.pojos.VesselEngineNodes
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.data.VesselEngineNodeDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject

class VesselEngineNodeControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "vessel.engine.node.query.list"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
    }
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
