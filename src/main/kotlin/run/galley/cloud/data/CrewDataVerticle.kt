package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.CrewFactory
import run.galley.cloud.sql.CrewSql

class CrewDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST_BY_ACTIVE_USER = "data.crew.query.list_by_active_user"
    const val LIST_BY_USER = "data.crew.query.list_by_user"
    const val CREATE = "data.crew.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST_BY_ACTIVE_USER, handler = ::listByActiveUser)
      vertx.eventBus().coConsumer(LIST_BY_USER, handler = ::listByUser)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
    }
  }

  private suspend fun listByActiveUser(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(CrewSql.listActive(request))

    val crew =
      results
        ?.map(CrewFactory::from)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(crew),
      ),
    )
  }

  private suspend fun listByUser(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(CrewSql.list(request))

    val crew =
      results
        ?.map(CrewFactory::from)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(crew),
      ),
    )
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(CrewSql.create(request))

    val crew = results?.firstOrNull()?.let(CrewFactory::from) ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_CAPTAIN)

    message.reply(EventBusDataResponse(DataPayload.one(crew)))
  }
}
