package run.galley.cloud.controller

import generated.jooq.tables.pojos.CharterProjects
import generated.jooq.tables.pojos.Charters
import generated.jooq.tables.references.CHARTERS
import generated.jooq.tables.references.CHARTER_COMPUTE_PLANS
import generated.jooq.tables.references.CHARTER_PROJECTS
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.Pagination
import nl.clicqo.api.SortDirection
import nl.clicqo.api.SortField
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
import run.galley.cloud.crew.CrewRole
import run.galley.cloud.crew.getCharters
import run.galley.cloud.data.CharterComputePlanDataVerticle
import run.galley.cloud.data.CharterDataVerticle
import run.galley.cloud.data.ProjectDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject
import java.util.UUID

class CharterControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "charter.query.list"
    const val GET = "charter.query.get"
    const val CREATE = "charter.cmd.create"
    const val PATCH = "charter.cmd.patch"
    const val DELETE = "charter.cmd.delete"
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

    val charterIds: List<UUID>? =
      if (apiRequest.crewRole != CrewRole.VESSEL_CAPTAIN) {
        apiRequest.user?.getCharters(vesselId) ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NO_ACCESS)
      } else {
        null
      }

    // Build data request with filters, sort, and pagination
    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            CHARTERS.VESSEL_ID eq vesselId
            if (!charterIds.isNullOrEmpty()) {
              CHARTERS.ID isIn charterIds
            }
          },
        sort = listOf(SortField("id", SortDirection.ASC)),
        pagination = Pagination(offset = 0, limit = 10),
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Charters>>(CharterDataVerticle.LIST, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    // Build data request with identifier
    val dataRequest =
      EventBusQueryDataRequest(
        identifiers = mapOf("id" to charterId.toString()),
        filters =
          filters {
            CHARTERS.VESSEL_ID eq vesselId
          },
        user = apiRequest.user?.principal(),
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Charters>>(CharterDataVerticle.GET, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId

    val charter = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)
    charter.put(CHARTERS.VESSEL_ID.name, vesselId)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = charter,
        userId = userId,
      )

    val createdCharter =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Charters>>(CharterDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()

    val dataResponse =
      createdCharter?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_CREATE_FAILURE)

    val computePlanRequest =
      EventBusCmdDataRequest(
        payload =
          JsonObject()
            .put(CHARTER_COMPUTE_PLANS.VESSEL_ID.name, createdCharter.vesselId.toString())
            .put(CHARTER_COMPUTE_PLANS.CHARTER_ID.name, createdCharter.id.toString()),
      )

    vertx
      .eventBus()
      .send(CharterComputePlanDataVerticle.CREATE_INITIAL, computePlanRequest)

    message.reply(
      EventBusApiResponse(
        data = dataResponse,
        httpStatus = HttpStatus.Created,
      ),
    )
  }

  private suspend fun patch(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    val dataRequest =
      EventBusCmdDataRequest(
        payload = apiRequest.body,
        identifier = charterId,
        filters =
          filters {
            CHARTERS.VESSEL_ID eq vesselId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Charters>>(CharterDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun delete(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    val projectsRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            CHARTER_PROJECTS.VESSEL_ID eq vesselId
            CHARTER_PROJECTS.CHARTER_ID eq charterId
          },
      )
    val activeProjects =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterProjects>>(ProjectDataVerticle.LIST, projectsRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
    if (activeProjects != null && activeProjects.isNotEmpty()) {
      throw ApiStatusReplyException(ApiStatus.CHARTER_DELETE_FAILURE_ACTIVE_PROJECTS)
    }

    val deleteRequest =
      EventBusCmdDataRequest(
        identifier = charterId,
        filters =
          filters {
            CHARTERS.VESSEL_ID eq vesselId
          },
      )

    // Idea: Optional, second call on delete can delete an archived charter

    vertx.eventBus().request<EventBusDataResponse<Charters>>(CharterDataVerticle.ARCHIVE, deleteRequest).coAwait()

    message.reply(EventBusApiResponse())
  }
}
