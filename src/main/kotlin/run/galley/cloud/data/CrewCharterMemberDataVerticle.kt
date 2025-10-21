package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.model.factory.CrewCharterMemberFactory
import run.galley.cloud.sql.CrewCharterMemberSql

class CrewCharterMemberDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST_BY_USER = "data.crew.charter.query.list_by_user"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST_BY_USER, handler = ::listByUser)
    }
  }

  private suspend fun listByUser(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(CrewCharterMemberSql.listByCrewId(request))

    val crew =
      results
        ?.map(CrewCharterMemberFactory::from)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(crew),
      ),
    )
  }
}
