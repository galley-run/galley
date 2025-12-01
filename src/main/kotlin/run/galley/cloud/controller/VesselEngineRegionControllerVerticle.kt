package run.galley.cloud.controller

import generated.jooq.tables.pojos.CharterProjects
import generated.jooq.tables.pojos.VesselEngineRegions
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
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
import run.galley.cloud.data.VesselEngineNodeDataVerticle
import run.galley.cloud.data.VesselEngineRegionDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject

class VesselEngineRegionControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "vessel.engine.region.query.list"
    const val GET = "vessel.engine.region.query.get"
    const val CREATE = "vessel.engine.region.cmd.create"
    const val PATCH = "vessel.engine.region.cmd.patch"
    const val DELETE = "vessel.engine.region.cmd.delete"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(DELETE, handler = ::delete)
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
        .request<EventBusDataResponse<VesselEngineRegions>>(
          VesselEngineRegionDataVerticle.LIST_BY_VESSEL_ID,
          dataRequest,
        ).coAwait()
        .body()
        .payload
        ?.toMany()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val regionId = apiRequest.regionId

    val dataRequest =
      EventBusQueryDataRequest(
        identifiers = mapOf("id" to regionId.toString()),
        filters =
          filters {
            VESSEL_ENGINE_REGIONS.VESSEL_ID eq vesselId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineRegions>>(
          VesselEngineRegionDataVerticle.GET,
          dataRequest,
        ).coAwait()
        .body()
        .payload
        ?.toOne()
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

  private suspend fun patch(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val regionId = apiRequest.regionId

    val region = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = region,
        identifier = regionId,
        filters =
          filters {
            VESSEL_ENGINE_REGIONS.VESSEL_ID eq vesselId
          },
      )

    val response =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselEngineRegions>>(VesselEngineRegionDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        ?.payload
        ?.toOne()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(response))
  }

  private suspend fun delete(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val regionId = apiRequest.regionId

    val projectsRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            VESSEL_ENGINE_NODES.VESSEL_ID eq vesselId
            VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID eq regionId
          },
      )
    val activeNodes =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterProjects>>(VesselEngineNodeDataVerticle.LIST_BY_VESSEL_ID, projectsRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
    if (activeNodes != null && activeNodes.isNotEmpty()) {
      throw ApiStatusReplyException(ApiStatus.VESSEL_REGION_DELETE_FAILURE_ACTIVE_NODES)
    }

    val deleteRequest =
      EventBusCmdDataRequest(
        identifier = regionId,
        filters =
          filters {
            VESSEL_ENGINE_REGIONS.VESSEL_ID eq vesselId
          },
      )

    vertx
      .eventBus()
      .request<EventBusDataResponse<VesselEngineRegions>>(VesselEngineRegionDataVerticle.DELETE, deleteRequest)
      .coAwait()

    message.reply(EventBusApiResponse())
  }
}
