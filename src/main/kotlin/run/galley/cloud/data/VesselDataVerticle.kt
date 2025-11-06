package run.galley.cloud.data

import generated.jooq.tables.pojos.VesselEngines
import generated.jooq.tables.references.VESSEL_ENGINES
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselFactory
import run.galley.cloud.sql.VesselSql

class VesselDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST = "data.vessel.query.list"
    const val CREATE = "data.vessel.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
    }
  }

  private suspend fun list(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselSql.list(request))

    val vessels =
      results
        ?.map(VesselFactory::from)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(vessels),
      ),
    )
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(VesselSql.create(request))

    val vessel =
      results?.firstOrNull()?.let(VesselFactory::from) ?: throw ApiStatusReplyException(ApiStatus.VESSEL_NOT_FOUND)

    val vesselEngineRequest =
      EventBusCmdDataRequest(
        JsonObject()
          .put(VESSEL_ENGINES.NAME.name, "default")
          .put(VESSEL_ENGINES.VESSEL_ID.name, vessel.id),
      )
    vertx
      .eventBus()
      .request<EventBusDataResponse<VesselEngines>>(VesselEngineDataVerticle.CREATE, vesselEngineRequest)
      .coAwait()

    message.reply(EventBusDataResponse(DataPayload.one(vessel)))
  }
}
