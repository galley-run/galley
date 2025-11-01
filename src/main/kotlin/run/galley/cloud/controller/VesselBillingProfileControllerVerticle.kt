package run.galley.cloud.controller

import generated.jooq.tables.pojos.VesselBillingProfile
import generated.jooq.tables.references.VESSEL_BILLING_PROFILE
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.eventbus.EventBusApiResponse
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import nl.clicqo.web.HttpStatus
import run.galley.cloud.ApiStatus
import run.galley.cloud.data.VesselBillingProfileDataVerticle

class VesselBillingProfileControllerVerticle :
  ControllerVerticle(),
  CoroutineEventBusSupport {
  companion object {
    const val CREATE = "vessel.billingProfile.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
    }
  }

  private suspend fun create(message: Message<EventBusApiRequest>) {
    val apiRequest = getApiRequest(message)
    val userId = apiRequest.user?.subject()?.toUUID() ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    val vesselId = apiRequest.vesselId
    val vesselBillingProfile = apiRequest.body ?: throw ApiStatusReplyException(ApiStatus.REQUEST_BODY_MISSING)

    vesselBillingProfile.put(VESSEL_BILLING_PROFILE.VESSEL_ID.name, vesselId)

    val dataRequest =
      EventBusCmdDataRequest(
        payload = vesselBillingProfile,
        userId = userId,
      )

    val dataResponse =
      vertx
        .eventBus()
        .request<EventBusDataResponse<VesselBillingProfile>>(VesselBillingProfileDataVerticle.CREATE, dataRequest)
        .coAwait()
        .body()
        .payload
        ?.toOne()
        ?.toJsonAPIResourceObject() ?: throw ApiStatusReplyException(ApiStatus.VESSEL_BILLING_PROFILE_CREATE_FAILURE)

    message.reply(
      EventBusApiResponse(
        data = dataResponse,
        httpStatus = HttpStatus.Created,
      ),
    )
  }
}
