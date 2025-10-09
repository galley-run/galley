package run.galley.cloud.controller

import generated.jooq.tables.pojos.Charters
import generated.jooq.tables.references.CHARTERS
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.Pagination
import nl.clicqo.api.SortDirection
import nl.clicqo.api.SortField
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.camelCaseToSnakeCase
import nl.clicqo.ext.toUUID
import nl.clicqo.web.HttpStatus
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.CharterDataVerticle
import run.galley.cloud.model.UserRole
import run.galley.cloud.model.getUserRole
import run.galley.cloud.model.toJsonAPIResourceObject

class CharterControllerVerticle : CoroutineVerticle() {
  companion object {
    const val LIST = "charter.query.list"
    const val GET = "charter.query.get"
    const val CREATE = "charter.cmd.create"
    const val PATCH = "charter.cmd.patch"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, LIST, ::list)
    vertx.eventBus().coroutineConsumer(coroutineContext, GET, ::get)
    vertx.eventBus().coroutineConsumer(coroutineContext, CREATE, ::create)
    vertx.eventBus().coroutineConsumer(coroutineContext, PATCH, ::patch)
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val userRole = apiRequest.user?.getUserRole()

    // Convert API query params to filters
    val filters = mutableMapOf<String, List<String>>()
    apiRequest.query?.forEach { (key, value) ->
      filters[key.camelCaseToSnakeCase()] = listOf(value.string)
    }

    val vesselId =
      apiRequest.identifiers
        ?.get("vesselId")
        ?.string
        ?.toUUID() ?: throw ApiStatus.VESSEL_ID_INCORRECT

    filters["vesselId"] = listOf(vesselId.toString())
    if (userRole != UserRole.VESSEL_CAPTAIN) {
      filters["id"] = listOf(apiRequest.user?.get<String>("charterIds") ?: throw ApiStatus.CHARTER_NO_ACCESS)
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

    // Convert back to API response
    message.reply(
      EventBusApiResponse(
        data = dataResponse.payload.toMany()!!.toJsonAPIResourceObject(),
      ),
    )
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()

    val vesselId = apiRequest.identifiers?.get("vesselId")?.string ?: throw ApiStatus.VESSEL_ID_INCORRECT
    val charterId =
      apiRequest.identifiers["charterId"]?.string
        ?: throw IllegalArgumentException("charterId is required")

    // Build data request with identifier
    val dataRequest =
      EventBusQueryDataRequest(
        identifiers = mapOf("id" to charterId),
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

    // Convert back to API response
    message.reply(
      EventBusApiResponse(
        data = dataResponse.payload.toOne()!!.toJsonAPIResourceObject(),
      ),
    )
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatus.USER_NOT_FOUND

    val vesselId =
      apiRequest.identifiers
        ?.get("vesselId")
        ?.string
        ?.toUUID() ?: throw ApiStatus.VESSEL_ID_INCORRECT

    val charter = apiRequest.body ?: throw ApiStatus.REQUEST_BODY_MISSING
    charter.put(CHARTERS.VESSEL_ID.name, vesselId)

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

    message.reply(
      EventBusApiResponse(
        data = dataResponse.payload.toOne()?.toJsonAPIResourceObject() ?: throw ApiStatus.CHARTER_CREATE_FAILURE,
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
        ?.toUUID() ?: throw ApiStatus.VESSEL_ID_INCORRECT
    val charterId =
      apiRequest.identifiers["charterId"]
        ?.string
        ?.toUUID() ?: throw ApiStatus.VESSEL_ID_INCORRECT

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

    message.reply(
      EventBusApiResponse(
        data = dataResponse.payload.toOne()?.toJsonAPIResourceObject() ?: throw ApiStatus.CHARTER_NOT_FOUND,
      ),
    )
  }
}
