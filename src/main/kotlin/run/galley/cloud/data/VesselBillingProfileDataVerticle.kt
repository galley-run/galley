package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselBillingProfileFactory
import run.galley.cloud.sql.VesselBillingProfileSql

class VesselBillingProfileDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.vessel.billingProfile.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
    }
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselBillingProfileSql.create(request))

    val vesselBillingProfile =
      results?.firstOrNull()?.let(VesselBillingProfileFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.VESSEL_BILLING_PROFILE_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(vesselBillingProfile)))
  }
}
