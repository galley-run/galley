package run.galley.cloud.data

import generated.jooq.tables.Sessions.Companion.SESSIONS
import generated.jooq.tables.pojos.Sessions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.coAwait
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.SessionFactory
import run.galley.cloud.sql.SessionSql

class SessionDataVerticle : PostgresDataVerticle() {
  companion object {
    const val CREATE = "data.session.cmd.create"
    const val REVOKE = "data.session.cmd.revoke"
    const val UPDATE = "data.session.cmd.update"
    const val PATCH = "data.session.cmd.patch"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(REVOKE, handler = ::revoke)
      vertx.eventBus().coConsumer(UPDATE, handler = ::update)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
    }
  }

  private suspend fun update(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()

    val replacingSessionResults = pool.execute(SessionSql.get(request))
    val currentSession =
      replacingSessionResults?.singleOrNull()?.let(SessionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.SESSION_NOT_FOUND)

    val results = pool.execute(SessionSql.create(request))
    val newSession =
      results
        ?.firstOrNull()
        ?.let(SessionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.SESSION_NOT_FOUND)

    val revokeRequest =
      EventBusCmdDataRequest(
        payload =
          JsonObject()
            .put(SESSIONS.REPLACED_BY_ID.name, newSession.id),
        identifier = currentSession.id,
        userId = currentSession.userId,
      )

    vertx.eventBus().request<EventBusDataResponse<Sessions>>(PATCH, revokeRequest).coAwait()

    message.reply(
      EventBusDataResponse(DataPayload.one(newSession)),
    )
  }

  private suspend fun revoke(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    // Revoke by User Id
    pool.execute(SessionSql.revoke(request))

    message.reply(
      EventBusDataResponse.noContent<Sessions>(),
    )
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()

    val results = pool.execute(SessionSql.create(request))
    val newSession =
      results?.firstOrNull()?.let(SessionFactory::from) ?: throw ApiStatusReplyException(ApiStatus.SESSION_NOT_FOUND)

    message.reply(
      EventBusDataResponse(DataPayload.one(newSession)),
    )
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()

    val results = pool.execute(SessionSql.update(request))
    val session =
      results?.firstOrNull()?.let(SessionFactory::from) ?: throw ApiStatusReplyException(ApiStatus.SESSION_NOT_FOUND)

    message.reply(
      EventBusDataResponse(DataPayload.one(session)),
    )
  }
}
