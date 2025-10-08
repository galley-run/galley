package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.Crew
import run.galley.cloud.sql.CrewSql

class CrewDataVerticle : PostgresDataVerticle() {
  companion object {
    const val ADDRESS_GET_BY_USER_AND_VESSEL = "data.crew.query.get_by_user_and_vessel"
    const val ADDRESS_LIST_ACTIVE = "data.crew.query.list_active"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET_BY_USER_AND_VESSEL, ::getByUserAndVessel)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_LIST_ACTIVE, ::listActive)
  }

  private suspend fun getByUserAndVessel(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CrewSql.getCrewMemberByUserAndVessel(request))

    val crew = results?.firstOrNull()?.let(Crew::from) ?: throw ApiStatus.USER_NOT_FOUND

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(crew),
      ),
    )
  }

  private suspend fun listActive(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CrewSql.listActive(request))

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(results?.map(Crew::from) ?: emptyList()),
      ),
    )
  }
}
