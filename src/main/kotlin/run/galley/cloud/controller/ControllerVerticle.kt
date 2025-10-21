package run.galley.cloud.controller

import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import java.util.UUID

open class ControllerVerticle : CoroutineVerticle() {
  protected fun getApiRequest(message: Message<EventBusApiRequest>): EventBusApiRequest = message.body()

  val EventBusApiRequest.vesselId: UUID
    get() =
      this.pathParams
        ?.get("vesselId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ID_INCORRECT)

  val EventBusApiRequest.charterId: UUID
    get() =
      this.pathParams
        ?.get("charterId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)
}
