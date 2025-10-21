package run.galley.cloud.controller

import generated.jooq.tables.references.CHARTERS
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.eventbus.EventBusApiRequest
import nl.clicqo.ext.CoroutineEventBusSupport
import nl.clicqo.ext.camelCaseToSnakeCase
import nl.clicqo.ext.coroutineEventBus
import nl.clicqo.ext.toUUID
import run.galley.cloud.ApiStatus
import run.galley.cloud.crew.UserRole
import run.galley.cloud.crew.getCharters
import run.galley.cloud.crew.getVessels
import java.util.UUID
import javax.annotation.processing.Messager

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

    filters[CHARTERS.VESSEL_ID.name] = listOf(requestedVesselId.toString())

    if (apiRequest.userRole != UserRole.VESSEL_CAPTAIN) {
      val charterIds = apiRequest.user?.getCharters(requestedVesselId) ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NO_ACCESS)

      filters[CHARTERS.ID.name] = charterIds.map(UUID::toString)
    }
  }
}
