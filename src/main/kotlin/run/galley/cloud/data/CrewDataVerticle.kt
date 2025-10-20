package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.Crew
import run.galley.cloud.sql.CrewSql

class CrewDataVerticle : PostgresDataVerticle() {
  companion object {
    const val GET_BY_USER_AND_VESSEL = "data.crew.query.get_by_user_and_vessel"
    const val LIST_ACTIVE = "data.crew.query.list_active"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(GET_BY_USER_AND_VESSEL, handler = ::getByUserAndVessel)
      vertx.eventBus().coConsumer(LIST_ACTIVE, handler = ::listActive)
    }
  }

  private suspend fun getByUserAndVessel(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CrewSql.getCrewMemberByUserAndVessel(request))

    val crew =
      results
        ?.firstOrNull()
        ?.let(Crew::from)
        ?: throw ApiStatusReplyException(ApiStatus.CREW_NO_VESSEL_MEMBER)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(crew),
      ),
    )
  }

  private suspend fun listActive(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.executePreparedQuery(CrewSql.listActive(request))

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(results?.map(Crew::from) ?: emptyList()),
      ),
    )
  }
}
