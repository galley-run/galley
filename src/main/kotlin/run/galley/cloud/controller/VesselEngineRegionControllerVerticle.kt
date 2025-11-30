package run.galley.cloud.controller

import generated.jooq.tables.pojos.VesselEngineRegions
import generated.jooq.tables.references.VESSEL_BILLING_PROFILE
import generated.jooq.tables.references.VESSEL_ENGINE_REGIONS
import io.vertx.core.eventbus.Message
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
import run.galley.cloud.data.VesselEngineRegionDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject

class VesselEngineRegionControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "vessel.engine.region.query.list"
    const val CREATE = "vessel.engine.region.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
    }
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId

    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            VESSEL_ENGINE_REGIONS.VESSEL_ID eq vesselId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineRegions>>(VesselEngineRegionDataVerticle.LIST_BY_VESSEL_ID, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val region = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    region.put(VESSEL_ENGINE_REGIONS.VESSEL_ID.name, vesselId)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = region,
        userId = userId,
      )

    val response =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineRegions>>(VesselEngineRegionDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        ?.payload
        ?.toOne()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(response, httpStatus = HttpStatus.Created))
  }
}
