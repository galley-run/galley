package run.galley.cloud.controller

import generated.jooq.tables.pojos.Vessels
import generated.jooq.tables.references.VESSELS
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.eventbus.filters
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.getVessels
import run.galley.cloud.data.VesselDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject

class VesselControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "vessel.query.list"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
    }
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselIds = apiRequest.user?.getVessels() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)

    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            VESSELS.ID isIn vesselIds
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Vessels>>(VesselDataVerticle.LIST, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(dataResponse))
  }
}
