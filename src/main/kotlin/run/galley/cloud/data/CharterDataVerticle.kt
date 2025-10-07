package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.model.Charter
import run.galley.cloud.sql.VesselSql

class CharterDataVerticle() : PostgresDataVerticle() {
  companion object {
    const val ADDRESS_LIST = "data.charter.query.list"
    const val ADDRESS_GET = "data.charter.query.get"
    const val ADDRESS_CREATE = "data.charter.cmd.create"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST, ::list)
//    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET, ::get)
  }

  private suspend fun list(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(VesselSql.listVessels(request))

    val charters = results?.map(Charter::from) ?: emptyList()

    val metadata = request.pagination?.let {
      JsonObject()
        .put("offset", it.offset)
        .put("limit", it.limit)
        .put("count", charters.size)
    }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(charters),
        metadata = metadata
      )
    )
  }

//  private suspend fun get(message: Message<EventBusDataRequest>) {
//    val request = message.body()
//    val results = pool.executePreparedQuery(VesselSql.getVessel(request))
//
//    val vessel = results?.firstOrNull()?.let { Vessel.from(it) }
//      ?: throw IllegalArgumentException("Vessel not found")
//
//    message.reply(
//      EventBusDataResponse(
//        payload = DataPayload.one(vessel)
//      )
//    )
//  }
}
