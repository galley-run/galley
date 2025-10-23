package run.galley.cloud.controller

import generated.jooq.tables.pojos.CharterProjects
import generated.jooq.tables.references.CHARTER_PROJECTS
import io.vertx.core.eventbus.Message
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
import run.galley.cloud.data.ProjectDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject

class ProjectControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "project.query.list"
    const val CREATE = "project.cmd.create"
    const val PATCH = "project.cmd.patch"
    const val DELETE = "project.cmd.delete"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(DELETE, handler = ::delete)
    }
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            CHARTER_PROJECTS.VESSEL_ID eq vesselId
            CHARTER_PROJECTS.CHARTER_ID eq charterId
          },
        sort = listOf(SortField("id", SortDirection.ASC)),
        pagination = Pagination(offset = 0, limit = 10),
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterProjects>>(ProjectDataVerticle.LIST, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany()
        ?.toJsonAPIResourceObject()

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    val project = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)
    project.put(CHARTER_PROJECTS.VESSEL_ID.name, vesselId)
    project.put(CHARTER_PROJECTS.CHARTER_ID.name, charterId)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = project,
        userId = userId,
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterProjects>>(ProjectDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject() ?: throw ApiStatusReplyException(ApiStatus.PROJECT_CREATE_FAILURE)

    message.reply(EventBusApiResponse(dataResponse, httpStatus = HttpStatus.Created))
  }

  private suspend fun patch(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val projectId = apiRequest.projectId

    val dataRequest =
      EventBusCmdDataRequest(
        payload = apiRequest.body,
        identifier = projectId,
        filters =
          filters {
            CHARTER_PROJECTS.VESSEL_ID eq vesselId
            CHARTER_PROJECTS.CHARTER_ID eq charterId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterProjects>>(ProjectDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject() ?: throw ApiStatusReplyException(ApiStatus.PROJECT_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun delete(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val projectId = apiRequest.projectId

    // Prerequisite:
    // Check if there are any active applications / databases / configs / secrets for this charter
    // TODO: Check if there are any active applications / databases for this project when the resp. data verticle are implemented

    val dataRequest =
      EventBusCmdDataRequest(
        identifier = projectId,
        filters =
          filters {
            CHARTER_PROJECTS.VESSEL_ID eq vesselId
            CHARTER_PROJECTS.CHARTER_ID eq charterId
          },
      )

    vertx
      .eventBus()
      .request<EventBusDataResponse<CharterProjects>>(ProjectDataVerticle.ARCHIVE, dataRequest)
      .coAwait()

    message.reply(EventBusApiResponse())
  }
}
