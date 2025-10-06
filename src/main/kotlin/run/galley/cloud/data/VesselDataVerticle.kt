package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.model.Vessel
import run.galley.cloud.sql.VesselSql

class VesselDataVerticle() : PostgresDataVerticle() {
  companion object {
    const val ADDRESS_LIST = "data.vessel.query.list"
    const val ADDRESS_GET = "data.vessel.query.get"
    const val ADDRESS_CREATE = "data.vessel.cmd.create"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST, ::list)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET, ::get)
  }

  private suspend fun list(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(VesselSql.listVessels(request))

    val vessels = results?.map { Vessel.from(it) } ?: emptyList()

    val metadata = request.pagination?.let {
      JsonObject()
        .put("offset", it.offset)
        .put("limit", it.limit)
        .put("count", vessels.size)
    }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(vessels),
        metadata = metadata
      )
    )
  }

  private suspend fun get(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(VesselSql.getVessel(request))

    val vessel = results?.firstOrNull()?.let { Vessel.from(it) }
      ?: throw IllegalArgumentException("Vessel not found")

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(vessel)
      )
    )
  }
}
