package run.galley.cloud.controller

import generated.jooq.tables.pojos.CharterProjects
import generated.jooq.tables.references.CHARTERS
import generated.jooq.tables.references.CHARTER_PROJECTS
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.api.Pagination
import nl.clicqo.api.SortDirection
import nl.clicqo.api.SortField
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.camelCaseToSnakeCase
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.UserRole
import run.galley.cloud.crew.getCharters
import run.galley.cloud.data.ProjectDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject
import java.util.UUID

class ProjectControllerVerticle :
  CoroutineVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "project.query.list"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
    }
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()

    // Convert API query params to filters
    val filters = mutableMapOf<String, List<String>>()
    apiRequest.query?.forEach { (key, value) ->
      filters[key.camelCaseToSnakeCase()] = listOf(value.string)
    }

    val requestedVesselId =
      apiRequest.pathParams
        ?.get("vesselId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)

    val requestedCharterId =
      apiRequest.pathParams["charterId"]
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)

    filters[CHARTER_PROJECTS.VESSEL_ID.name] = listOf(requestedVesselId.toString())
    filters[CHARTER_PROJECTS.CHARTER_ID.name] = listOf(requestedCharterId.toString())

    val dataRequest =
      EventBusQueryDataRequest(
        filters = filters,
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
}
