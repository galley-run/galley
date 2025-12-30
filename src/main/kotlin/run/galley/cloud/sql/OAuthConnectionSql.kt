package run.galley.cloud.sql

import generated.jooq.tables.records.OAuthConnectionsRecord
import generated.jooq.tables.references.OAUTH_CONNECTIONS
import generated.jooq.tables.references.OAUTH_CONNECTION_GRANTS
import generated.jooq.tables.references.OAUTH_CREDENTIALS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.applyPagination
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toRecord
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.OAuthConnectionFactory
import run.galley.cloud.model.factory.OAuthConnectionGrantFactory
import run.galley.cloud.model.factory.OAuthCredentialFactory
import java.util.UUID

object OAuthConnectionSql {
  fun listOAuthConnections(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(OAUTH_CONNECTIONS)
      .applyConditions(*conditions)
      .applyPagination(request.pagination)
  }

  fun getOAuthConnection(request: EventBusQueryDataRequest): Query {
    val identifier = request.identifiers["id"]
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(OAUTH_CONNECTIONS)
      .where(OAUTH_CONNECTIONS.ID.eq(identifier?.toUUID()))
      .applyConditions(*conditions)
  }

  fun createOAuthConnection(request: EventBusCmdDataRequest): Query {
    val payload = request.payload ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(OAUTH_CONNECTIONS)
      .set(OAuthConnectionFactory.toRecord(payload))
      .onConflictOnConstraint(
        org.jooq.impl.DSL
          .name("uq_oauth_connections_vessel_charter_provider_type"),
      ).doUpdate()
      .set(
        OAUTH_CONNECTIONS.STATUS,
        org.jooq.impl.DSL
          .excluded(OAUTH_CONNECTIONS.STATUS),
      ).set(
        OAUTH_CONNECTIONS.DISPLAY_NAME,
        org.jooq.impl.DSL
          .excluded(OAUTH_CONNECTIONS.DISPLAY_NAME),
      ).set(
        OAUTH_CONNECTIONS.CREATED_BY_USER_ID,
        org.jooq.impl.DSL
          .excluded(OAUTH_CONNECTIONS.CREATED_BY_USER_ID),
      ).set(
        OAUTH_CONNECTIONS.SCOPES,
        org.jooq.impl.DSL
          .excluded(OAUTH_CONNECTIONS.SCOPES),
      ).set(
        OAUTH_CONNECTIONS.PROVIDER_ACCOUNT_ID,
        org.jooq.impl.DSL
          .excluded(OAUTH_CONNECTIONS.PROVIDER_ACCOUNT_ID),
      ).set(
        OAUTH_CONNECTIONS.LAST_VALIDATED_AT,
        org.jooq.impl.DSL
          .excluded(OAUTH_CONNECTIONS.LAST_VALIDATED_AT),
      ).returning()
  }

  fun patchOAuthConnection(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier

    return Jooq.postgres
      .update(OAUTH_CONNECTIONS)
      .set(payload.toRecord<OAuthConnectionsRecord>(OAUTH_CONNECTIONS))
      .where(OAUTH_CONNECTIONS.ID.eq(identifier))
      .applyConditions(*buildConditions(request.filters))
      .returning()
  }

  fun deleteOAuthConnection(request: EventBusCmdDataRequest): Query =
    Jooq.postgres
      .deleteFrom(OAUTH_CONNECTIONS)
      .where(OAUTH_CONNECTIONS.ID.eq(request.identifier))
      .applyConditions(*buildConditions(request.filters))

  fun createOAuthCredential(request: EventBusCmdDataRequest): Query {
    val payload = request.payload ?: throw ApiStatus.REQUEST_BODY_MISSING

    // First delete any existing credentials for this connection
    // This ensures we replace old credentials when replacing a connection
    return Jooq.postgres
      .insertInto(OAUTH_CREDENTIALS)
      .set(OAuthCredentialFactory.toRecord(payload))
      .returning()
  }

  fun deleteOAuthCredentialsByConnectionId(connectionId: UUID): Query =
    Jooq.postgres
      .deleteFrom(OAUTH_CREDENTIALS)
      .where(OAUTH_CREDENTIALS.CONNECTION_ID.eq(connectionId))

  fun createOAuthGrant(request: EventBusCmdDataRequest): Query {
    val payload = request.payload ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(OAUTH_CONNECTION_GRANTS)
      .set(OAuthConnectionGrantFactory.toRecord(payload))
      .onConflictOnConstraint(
        org.jooq.impl.DSL
          .name("uq_oauth_grants_principal"),
      ).doNothing()
      .returning()
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          OAUTH_CONNECTIONS.ID.name -> OAUTH_CONNECTIONS.ID.`in`(values.map { UUID.fromString(it) })
          OAUTH_CONNECTIONS.VESSEL_ID.name -> OAUTH_CONNECTIONS.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          OAUTH_CONNECTIONS.CHARTER_ID.name -> OAUTH_CONNECTIONS.CHARTER_ID.`in`(values.map { UUID.fromString(it) })
          OAUTH_CONNECTIONS.PROVIDER.name -> OAUTH_CONNECTIONS.PROVIDER.`in`(values)
          OAUTH_CONNECTIONS.TYPE.name -> OAUTH_CONNECTIONS.TYPE.`in`(values)
          OAUTH_CONNECTIONS.STATUS.name -> OAUTH_CONNECTIONS.STATUS.`in`(values)
          OAUTH_CONNECTIONS.CREATED_BY_USER_ID.name -> OAUTH_CONNECTIONS.CREATED_BY_USER_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
