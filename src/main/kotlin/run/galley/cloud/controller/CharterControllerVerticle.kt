package run.galley.cloud.controller

import generated.jooq.tables.pojos.Charters
import generated.jooq.tables.references.CHARTERS
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
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
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.camelCaseToSnakeCase
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import nl.clicqo.web.HttpStatus
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.CharterDataVerticle
import run.galley.cloud.model.UserRole
import run.galley.cloud.model.getCrewAccess
import run.galley.cloud.model.toJsonAPIResourceObject

class CharterControllerVerticle :
  CoroutineVerticle(),
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
    val apiRequest = message.body()

    // Convert API query params to filters
    val filters = mutableMapOf<String, List<String>>()
    apiRequest.query?.forEach { (key, value) ->
      filters[key.camelCaseToSnakeCase()] = listOf(value.string)
    }

    val vesselId =
      apiRequest.identifiers
        ?.get("vesselId")
        ?.string
        ?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)

    val userRole = apiRequest.user?.getCrewAccess(vesselId)

    filters["vesselId"] = listOf(vesselId.toString())
    if (userRole != UserRole.VESSEL_CAPTAIN) {
      filters["id"] =
        listOf(apiRequest.user?.get<String>("charterIds") ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NO_ACCESS))
      TODO("Not implemented")
    }

    // Build data request with filters, sort, and pagination
    val dataRequest =
      EventBusQueryDataRequest(
        filters = filters,
        sort = listOf(SortField("id", SortDirection.ASC)),
        pagination = Pagination(offset = 0, limit = 10),
        user = apiRequest.user.principal(),
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

    // Convert back to API response
    message.reply(
      EventBusApiResponse(
        data = dataResponse,
      ),
    )
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()

    val vesselId =
      apiRequest.identifiers?.get("vesselId")?.string ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)
    val charterId =
      apiRequest.identifiers["charterId"]?.string?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)

    // Build data request with identifier
    val dataRequest =
      EventBusQueryDataRequest(
        identifiers = mapOf("id" to charterId.toString()),
        filters =
          mapOf(
            CHARTERS.VESSEL_ID.name to listOf(vesselId),
          ),
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
        ?.toJsonAPIResourceObject() ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    // Convert back to API response
    message.reply(
      EventBusApiResponse(
        data = dataResponse,
      ),
    )
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId =
      apiRequest.identifiers
        ?.get("vesselId")
        ?.string
        ?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)

    val charter = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)
    // Ensure UUID is serialized as String for downstream getUUID extraction
    charter.put(CHARTERS.VESSEL_ID.name, vesselId.toString())

    val dataRequest =
      EventBusCmdDataRequest(
        payload = charter,
        userId = userId,
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Charters>>(CharterDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject() ?: throw ApiStatusReplyException(ApiStatus.CHARTER_CREATE_FAILURE)

    message.reply(
      EventBusApiResponse(
        data = dataResponse,
        httpStatus = HttpStatus.Created,
      ),
    )
  }

  private suspend fun patch(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val vesselId =
      apiRequest.identifiers
        ?.get("vesselId")
        ?.string
        ?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)
    val charterId =
      apiRequest.identifiers["charterId"]
        ?.string
        ?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = apiRequest.body,
        identifier = charterId,
        filters =
          mapOf(
            CHARTERS.VESSEL_ID.name to listOf(vesselId.toString()),
          ),
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<Charters>>(CharterDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject() ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    message.reply(
      EventBusApiResponse(
        data = dataResponse,
      ),
    )
  }

  private suspend fun delete(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val vesselId =
      apiRequest.identifiers
        ?.get("vesselId")
        ?.string
        ?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)
    val charterId =
      apiRequest.identifiers["charterId"]?.string?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)

    // Prerequisite:
    // Check if there are any active projects for this charter
    // TODO: Check if there are any active projects for this charter when the projects data verticle is implemented

    val deleteRequest =
      EventBusCmdDataRequest(
        identifier = charterId,
        filters = mapOf(CHARTERS.VESSEL_ID.name to listOf(vesselId.toString())),
      )

    // Idea: Optional, second call on delete can delete an archived charter

    vertx
      .eventBus()
      .request<EventBusDataResponse<Charters>>(CharterDataVerticle.ARCHIVE, deleteRequest)
      .coAwait()
      .body()

    message.reply(EventBusApiResponse())
  }
}
