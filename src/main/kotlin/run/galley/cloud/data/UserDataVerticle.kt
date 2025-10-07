package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.data.DataPayload
import nl.clicqo.data.executePreparedQuery
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.kleilokaal.queue.modules.coroutineConsumer
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.User
import run.galley.cloud.sql.UserSql

class UserDataVerticle() : PostgresDataVerticle() {
  companion object {
    const val ADDRESS_GET = "data.user.query.get"
    const val ADDRESS_GET_BY_EMAIL = "data.user.query.get_by_email"
  }

  override suspend fun start() {
    super.start()

    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET, ::get)
    vertx.eventBus().coroutineConsumer(coroutineContext, ADDRESS_GET_BY_EMAIL, ::getByEmail)
  }

  private suspend fun get(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val user = pool.executePreparedQuery(UserSql.getUser(request))?.firstOrNull()?.let(User::from)
      ?: throw ApiStatus.USER_NOT_FOUND

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(user)
      )
    )
  }

  private suspend fun getByEmail(message: Message<EventBusDataRequest>) {
    val request = message.body()
    val user = pool.executePreparedQuery(UserSql.getUserByEmail(request))?.firstOrNull()?.let(User::from)
      ?: throw ApiStatus.USER_NOT_FOUND

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(user)
      )
    )
  }
}
