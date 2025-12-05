package run.galley.cloud.controller

import generated.jooq.tables.pojos.CharterComputePlans
import generated.jooq.tables.references.CHARTER_COMPUTE_PLANS
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
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
import run.galley.cloud.data.CharterComputePlanDataVerticle
import run.galley.cloud.model.toJsonAPIResourceObject

class CharterComputePlanControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val LIST = "charter.computePlan.query.list"
    const val GET = "charter.computePlan.query.get"
    const val CREATE = "charter.computePlan.cmd.create"
    const val PATCH = "charter.computePlan.cmd.patch"
    const val DELETE = "charter.computePlan.cmd.delete"
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
    val charterId = apiRequest.charterId

    val dataRequest =
      EventBusQueryDataRequest(
        filters =
          filters {
            CHARTER_COMPUTE_PLANS.VESSEL_ID eq vesselId
            CHARTER_COMPUTE_PLANS.CHARTER_ID eq charterId
          },
        sort = listOf(SortField(CHARTER_COMPUTE_PLANS.NAME.name, SortDirection.ASC)),
        pagination = Pagination(offset = 0, limit = 100),
      )

    val computePlans =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterComputePlans>>(CharterComputePlanDataVerticle.LIST, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toMany() ?: emptyList()

    val dataResponse = JsonArray(computePlans.map { it.toJsonAPIResourceObject() })

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun get(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val computePlanId = apiRequest.computePlanId

    val dataRequest =
      EventBusQueryDataRequest(
        identifiers = mapOf(CHARTER_COMPUTE_PLANS.ID.name to computePlanId.toString()),
        filters =
          filters {
            CHARTER_COMPUTE_PLANS.VESSEL_ID eq vesselId
            CHARTER_COMPUTE_PLANS.CHARTER_ID eq charterId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterComputePlans>>(CharterComputePlanDataVerticle.GET, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId

    val computePlan = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    // Extract nested fields from the request body according to OpenAPI spec
    val requests = computePlan.getJsonObject("requests")
    val limits = computePlan.getJsonObject("limits")
    val billing = computePlan.getJsonObject("billing")

    // Flatten the nested structure for database storage
    computePlan.put(CHARTER_COMPUTE_PLANS.VESSEL_ID.name, vesselId)
    computePlan.put(CHARTER_COMPUTE_PLANS.CHARTER_ID.name, charterId)
    computePlan.put(CHARTER_COMPUTE_PLANS.REQUESTS_CPU.name, requests?.getString("cpu"))
    computePlan.put(CHARTER_COMPUTE_PLANS.REQUESTS_MEMORY.name, requests?.getString("memory"))
    computePlan.put(CHARTER_COMPUTE_PLANS.LIMITS_CPU.name, limits?.getString("cpu"))
    computePlan.put(CHARTER_COMPUTE_PLANS.LIMITS_MEMORY.name, limits?.getString("memory"))
    computePlan.put(CHARTER_COMPUTE_PLANS.BILLING_ENABLED.name, billing?.getBoolean("enabled") ?: false)
    computePlan.put(CHARTER_COMPUTE_PLANS.BILLING_PERIOD.name, billing?.getString("period"))
    computePlan.put(CHARTER_COMPUTE_PLANS.BILLING_UNIT_PRICE.name, billing?.getString("unitPrice"))

    // Remove nested objects as they're now flattened
    computePlan.remove("requests")
    computePlan.remove("limits")
    computePlan.remove("billing")

    val dataRequest =
      EventBusCmdDataRequest(
        payload = computePlan,
        userId = userId,
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterComputePlans>>(CharterComputePlanDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_CREATE_FAILURE)

    message.reply(EventBusApiResponse(dataResponse, httpStatus = HttpStatus.Ok))
  }

  private suspend fun patch(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val computePlanId = apiRequest.computePlanId

    val computePlan = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    // Extract and flatten nested fields if present
    val requests = computePlan.getJsonObject("requests")
    val limits = computePlan.getJsonObject("limits")
    val billing = computePlan.getJsonObject("billing")

    if (requests != null) {
      computePlan.put(CHARTER_COMPUTE_PLANS.REQUESTS_CPU.name, requests.getString("cpu"))
      computePlan.put(CHARTER_COMPUTE_PLANS.REQUESTS_MEMORY.name, requests.getString("memory"))
      computePlan.remove("requests")
    }

    if (limits != null) {
      computePlan.put(CHARTER_COMPUTE_PLANS.LIMITS_CPU.name, limits.getString("cpu"))
      computePlan.put(CHARTER_COMPUTE_PLANS.LIMITS_MEMORY.name, limits.getString("memory"))
      computePlan.remove("limits")
    }

    if (billing != null) {
      computePlan.put(CHARTER_COMPUTE_PLANS.BILLING_ENABLED.name, billing.getBoolean("enabled"))
      computePlan.put(CHARTER_COMPUTE_PLANS.BILLING_PERIOD.name, billing.getString("period"))
      computePlan.put(CHARTER_COMPUTE_PLANS.BILLING_UNIT_PRICE.name, billing.getString("unitPrice"))
      computePlan.remove("billing")
    }

    val dataRequest =
      EventBusCmdDataRequest(
        payload = computePlan,
        identifier = computePlanId,
        filters =
          filters {
            CHARTER_COMPUTE_PLANS.VESSEL_ID eq vesselId
            CHARTER_COMPUTE_PLANS.CHARTER_ID eq charterId
          },
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<CharterComputePlans>>(CharterComputePlanDataVerticle.PATCH, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject()
        ?: throw ApiStatusReplyException(ApiStatus.COMPUTE_PLAN_NOT_FOUND)

    message.reply(EventBusApiResponse(dataResponse))
  }

  private suspend fun delete(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)

    val vesselId = apiRequest.vesselId
    val charterId = apiRequest.charterId
    val computePlanId = apiRequest.computePlanId

    val dataRequest =
      EventBusCmdDataRequest(
        identifier = computePlanId,
        filters =
          filters {
            CHARTER_COMPUTE_PLANS.VESSEL_ID eq vesselId
            CHARTER_COMPUTE_PLANS.CHARTER_ID eq charterId
          },
      )

    vertx
      .eventBus()
      .request<EventBusDataResponse<CharterComputePlans>>(CharterComputePlanDataVerticle.ARCHIVE, dataRequest)
      .coAwait()

    message.reply(EventBusApiResponse())
  }
}
