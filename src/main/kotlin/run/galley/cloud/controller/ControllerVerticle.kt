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

  val EventBusApiRequest.nodeId: UUID
    get() =
      this.pathParams
        ?.get("nodeId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_NODE_ID_INCORRECT)

  val EventBusApiRequest.charterId: UUID
    get() =
      this.pathParams
        ?.get("charterId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_ID_INCORRECT)

  val EventBusApiRequest.projectId: UUID
    get() =
      this.pathParams
        ?.get("projectId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.PROJECT_ID_INCORRECT)

  val EventBusApiRequest.regionId: UUID
    get() =
      this.pathParams
        ?.get("regionId")
        ?.string
        ?.toUUID()
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_ENGINE_REGION_ID_INCORRECT)
}
