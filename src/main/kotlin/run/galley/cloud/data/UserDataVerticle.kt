package run.galley.cloud.data

import generated.jooq.tables.pojos.SignUpInquiries
import io.vertx.core.eventbus.Message
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.UserFactory
import run.galley.cloud.sql.SignUpInquirySql
import run.galley.cloud.sql.UserSql

class UserDataVerticle : PostgresDataVerticle() {
  companion object {
    const val GET = "data.user.query.get"
    const val GET_BY_EMAIL = "data.user.query.get_by_email"
    const val CREATE = "data.user.cmd.create"
    const val SIGN_UP_INQUIRY = "data.user.cmd.create.inquiry"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(GET_BY_EMAIL, handler = ::getByEmail)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(SIGN_UP_INQUIRY, handler = ::signUpInquiry)
    }
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val user =
      pool.execute(UserSql.getUser(request))?.firstOrNull()?.let(UserFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(DataPayload.one(user)),
    )
  }

  private suspend fun getByEmail(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val user =
      pool.execute(UserSql.getUserByEmail(request))?.firstOrNull()?.let(UserFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(DataPayload.one(user)),
    )
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val user =
      pool.execute(UserSql.create(request))?.firstOrNull()?.let(UserFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.USER_NOT_FOUND)

    message.reply(
      EventBusDataResponse(DataPayload.one(user)),
    )
  }

  private suspend fun signUpInquiry(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    pool.execute(SignUpInquirySql.create(request)) ?: throw ApiStatusReplyException(ApiStatus.SIGN_UP_INQUIRY_STORE_FAILED)

    message.reply(EventBusDataResponse<SignUpInquiries>())
  }
}
