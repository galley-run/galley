package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.model.factory.CrewFactory
import run.galley.cloud.sql.CrewSql

class CrewDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST_BY_USER = "data.crew.query.list_by_user"
    const val LIST_ACTIVE = "data.crew.query.list_active"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST_BY_USER, handler = ::listByUser)
      vertx.eventBus().coConsumer(LIST_ACTIVE, handler = ::listActive)
    }
  }

  private suspend fun listByUser(message: Message<EventBusQueryDataRequest>) {
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

  private suspend fun listActive(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(CrewSql.listActive(request))

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(results?.map(CrewFactory::from) ?: emptyList()),
      ),
    )
  }
}
