package run.galley.cloud.data

import generated.jooq.tables.pojos.Charters
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.model.Charter
import run.galley.cloud.sql.CharterSql

class CharterDataVerticle : PostgresDataVerticle() {
  companion object {
    const val ADDRESS_LIST = "data.charter.query.list"
    const val ADDRESS_GET = "data.charter.query.get"
    const val ADDRESS_CREATE = "data.charter.cmd.create"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST, ::list)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET, ::get)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_CREATE, ::create)
  }

  private suspend fun list(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CharterSql.listCharters(request))

    val charters = results?.map(Charter::from) ?: emptyList()

    val metadata =
      request.pagination?.let {
        JsonObject()
          .put("offset", it.offset)
          .put("limit", it.limit)
          .put("count", charters.size)
      }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(charters),
        metadata = metadata,
      ),
    )
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CharterSql.getCharter(request))

    val charter =
      results?.firstOrNull()?.let { Charter.from(it) }
        ?: throw IllegalArgumentException("Charter not found")

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(charter),
      ),
    )
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest<Charters>>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CharterSql.createCharter(request))

    val charter = results?.firstOrNull()?.let(Charter::from) ?: throw IllegalArgumentException("Charter not found")

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(charter),
      ),
    )
  }
}
