package run.galley.cloud.controller

import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.Pagination
import nl.clicqo.eventbus.SortDirection
import nl.clicqo.eventbus.SortField
import nl.kleilokaal.queue.modules.coroutineConsumer
import org.slf4j.LoggerFactory
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.VesselDataVerticle
import run.galley.cloud.model.UserRole
import run.galley.cloud.model.getUserRole

class VesselControllerVerticle : CoroutineVerticle() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val ADDRESS_LIST = "vessel.query.list"
    const val ADDRESS_GET = "vessel.query.get"
    const val ADDRESS_CREATE = "vessel.cmd.create"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST, ::list)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET, ::get)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_CREATE, ::create)
  }

  private suspend fun list(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()
    val userRole = apiRequest.user?.getUserRole()

    val isAllowed = when (userRole) {
//      UserRole.VESSEL_CAPTAIN -> true
      UserRole.CHARTER_CAPTAIN -> true
      else -> false
    }

    if (!isAllowed) {
      throw ApiStatus.USER_ROLE_FORBIDDEN
    }

    // Convert API query params to filters
    val filters = mutableMapOf<String, List<String>>()
    apiRequest.query?.forEach { (key, value) ->
      filters[key] = listOf(value.string)
    }

    // Build data request with filters, sort, and pagination
    val dataRequest = EventBusDataRequest(
      filters = filters,
      sort = listOf(SortField("name", SortDirection.ASC)),
      pagination = Pagination(offset = 0, limit = 50),
      user = apiRequest.user?.principal()
    )

    val dataResponse = vertx.eventBus()
      .request<EventBusDataResponse>(VesselDataVerticle.ADDRESS_LIST, dataRequest)
      .coAwait()
      .body()

    // Convert back to API response
    message.reply(
      EventBusApiResponse(
        payload = dataResponse.payload
      )
    )
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = message.body()

    // Extract vesselId from path identifiers
    val vesselId = apiRequest.identifiers?.get("vesselId")?.string
      ?: throw IllegalArgumentException("vesselId is required")

    // Build data request with identifier
    val dataRequest = EventBusDataRequest(
      identifiers = mapOf("vesselId" to vesselId),
      user = apiRequest.user?.principal()
    )

    val dataResponse = vertx.eventBus()
      .request<EventBusDataResponse>(VesselDataVerticle.ADDRESS_GET, dataRequest)
      .coAwait()
      .body()

    // Convert back to API response
    message.reply(
      EventBusApiResponse(
        payload = dataResponse.payload
      )
    )
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    throw ApiStatus.VESSEL_INSERT_FAILED
  }
}
