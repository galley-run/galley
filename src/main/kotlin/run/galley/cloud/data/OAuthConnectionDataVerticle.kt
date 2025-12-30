package run.galley.cloud.data

import generated.jooq.tables.pojos.OAuthConnections
import generated.jooq.tables.pojos.OAuthCredentials
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatusReplyException
import nl.clicqo.data.DataPayload
import nl.clicqo.data.execute
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusDataResponse
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.coroutineEventBus
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.OAuthConnectionFactory
import run.galley.cloud.model.factory.OAuthCredentialFactory
import run.galley.cloud.sql.OAuthConnectionSql

class OAuthConnectionDataVerticle : PostgresDataVerticle() {
  companion object {
    const val LIST = "data.oauth_connection.query.list"
    const val GET = "data.oauth_connection.query.get"
    const val CREATE = "data.oauth_connection.cmd.create"
    const val PATCH = "data.oauth_connection.cmd.patch"
    const val DELETE = "data.oauth_connection.cmd.delete"
    const val CREATE_CREDENTIAL = "data.oauth_credential.cmd.create"
    const val DELETE_CREDENTIALS = "data.oauth_credential.cmd.delete_by_connection"
    const val GET_CREDENTIALS = "data.oauth_credential.query.get_by_connection"
    const val CREATE_GRANT = "data.oauth_grant.cmd.create"
  }

  override suspend fun start() {
    super.start()

    coroutineEventBus {
      vertx.eventBus().coConsumer(LIST, handler = ::list)
      vertx.eventBus().coConsumer(GET, handler = ::get)
      vertx.eventBus().coConsumer(CREATE, handler = ::create)
      vertx.eventBus().coConsumer(PATCH, handler = ::patch)
      vertx.eventBus().coConsumer(DELETE, handler = ::delete)
      vertx.eventBus().coConsumer(CREATE_CREDENTIAL, handler = ::createCredential)
      vertx.eventBus().coConsumer(DELETE_CREDENTIALS, handler = ::deleteCredentials)
      vertx.eventBus().coConsumer(GET_CREDENTIALS, handler = ::getCredentials)
      vertx.eventBus().coConsumer(CREATE_GRANT, handler = ::createGrant)
    }
  }

  private suspend fun list(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(OAuthConnectionSql.listOAuthConnections(request))

    val connections = results?.map(OAuthConnectionFactory::from) ?: emptyList()

    val metadata =
      request.pagination?.let {
        JsonObject()
          .put("offset", it.offset)
          .put("limit", it.limit)
          .put("count", connections.size)
      }

    message.reply(
      EventBusDataResponse(
        payload = DataPayload.many(connections),
        metadata = metadata,
      ),
    )
  }

  private suspend fun get(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val results = pool.execute(OAuthConnectionSql.getOAuthConnection(request))

    val connection =
      results
        ?.firstOrNull()
        ?.let(OAuthConnectionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(connection)))
  }

  private suspend fun create(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(OAuthConnectionSql.createOAuthConnection(request))

    val connection =
      results?.firstOrNull()?.let(OAuthConnectionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_CREATE_FAILURE)

    message.reply(EventBusDataResponse(DataPayload.one(connection)))
  }

  private suspend fun patch(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val results = pool.execute(OAuthConnectionSql.patchOAuthConnection(request))

    val connection =
      results?.firstOrNull()?.let(OAuthConnectionFactory::from)
        ?: throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_NOT_FOUND)

    message.reply(EventBusDataResponse(DataPayload.one(connection)))
  }

  private suspend fun delete(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    val updated = pool.execute(OAuthConnectionSql.deleteOAuthConnection(request))

    if (updated?.rowCount() == 0) {
      throw ApiStatusReplyException(ApiStatus.OAUTH_CONNECTION_NOT_FOUND)
    }

    message.reply(EventBusDataResponse.noContent<OAuthConnections>())
  }

  private suspend fun createCredential(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    pool.execute(OAuthConnectionSql.createOAuthCredential(request))
    message.reply(EventBusDataResponse.noContent<OAuthConnections>())
  }

  private suspend fun deleteCredentials(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    pool.execute(OAuthConnectionSql.deleteOAuthCredentialsByConnectionId(request.identifier!!))
    message.reply(EventBusDataResponse.noContent<OAuthConnections>())
  }

  private suspend fun getCredentials(message: Message<EventBusQueryDataRequest>) {
    val request = message.body()
    val connectionId =
      request.identifiers["connection_id"]?.let { java.util.UUID.fromString(it) }
        ?: throw ApiStatusReplyException(ApiStatus.ID_MISSING)

    val results = pool.execute(OAuthConnectionSql.getOAuthCredentialsByConnectionId(connectionId))
    val row = results?.firstOrNull()

    if (row == null) {
      message.reply(EventBusDataResponse.noContent<OAuthCredentials>())
      return
    }

    val credentials = OAuthCredentialFactory.from(row)

    message.reply(EventBusDataResponse(DataPayload.one(credentials)))
  }

  private suspend fun createGrant(message: Message<EventBusCmdDataRequest>) {
    val request = message.body()
    pool.execute(OAuthConnectionSql.createOAuthGrant(request))
    message.reply(EventBusDataResponse.noContent<OAuthConnections>())
  }
}
