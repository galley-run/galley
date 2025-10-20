package run.galley.cloud.data

import generated.jooq.tables.pojos.Charters
import io.vertx.core.eventbus.Message
import io.vertx.core.internal.logging.LoggerFactory
import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.Charter
import run.galley.cloud.sql.CharterSql

class CharterDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST = "data.charter.query.list"
    const val GET = "data.charter.query.get"
    const val CREATE = "data.charter.cmd.create"
    const val PATCH = "data.charter.cmd.patch"
    const val ARCHIVE = "data.charter.cmd.archive"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(ARCHIVE, handler = ::archive)
    }
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
      results
        ?.firstOrNull()
        ?.let(Charter::from)
        ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(charter),
      ),
    )
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CharterSql.createCharter(request))

    val charter = results?.firstOrNull()?.let(Charter::from) ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(charter),
      ),
    )
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CharterSql.patchCharter(request))

    val charter = results?.firstOrNull()?.let(Charter::from) ?: throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(charter),
      ),
    )
  }

  private suspend fun archive(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val updated = pool.executePreparedQuery(CharterSql.archiveCharter(request))

    if (updated?.rowCount() == 0) {
      throw ApiStatusReplyException(ApiStatus.CHARTER_NOT_FOUND)
    }

    message.reply(
      EventBusDataResponse.noContent<Charters>(),
    )
  }
}
