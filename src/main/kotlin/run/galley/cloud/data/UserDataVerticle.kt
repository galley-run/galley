package run.galley.cloud.data

import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.UserFactory
import run.galley.cloud.sql.UserSql

class UserDataVerticle : PostgresDataVerticle() {
  companion object {
    const val GET = "data.user.query.get"
    const val GET_BY_EMAIL = "data.user.query.get_by_email"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(GET_BY_EMAIL, handler = ::getByEmail)
    }
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val user =
      pool.execute(UserSql.getUser(request))?.firstOrNull()?.let(UserFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(user),
      ),
    )
  }

  private suspend fun getByEmail(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val user =
      pool.execute(UserSql.getUserByEmail(request))?.firstOrNull()?.let(UserFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.one(user),
      ),
    )
  }
}
