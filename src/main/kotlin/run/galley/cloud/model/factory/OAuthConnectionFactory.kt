package run.galley.cloud.model.factory

import generated.jooq.enums.OAuthConnectionStatus
import generated.jooq.enums.OAuthConnectionType
import generated.jooq.enums.OAuthProvider
import generated.jooq.tables.pojos.OAuthConnections
import generated.jooq.tables.records.OAuthConnectionsRecord
import generated.jooq.tables.references.OAUTH_CONNECTIONS
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import nl.clicqo.ext.getUUID
import java.time.OffsetDateTime

object OAuthConnectionFactory {
  fun from(row: Row) =
    OAuthConnections(
      id = row.getUUID(OAUTH_CONNECTIONS.ID.name),
      vesselId = row.getUUID(OAUTH_CONNECTIONS.VESSEL_ID.name),
      charterId = row.getUUID(OAUTH_CONNECTIONS.CHARTER_ID.name),
      type = row.getString(OAUTH_CONNECTIONS.TYPE.name)?.let(OAuthConnectionType::valueOf),
      provider = row.getString(OAUTH_CONNECTIONS.PROVIDER.name)?.let(OAuthProvider::valueOf),
      status = row.getString(OAUTH_CONNECTIONS.STATUS.name)?.let(OAuthConnectionStatus::valueOf),
      displayName = row.getString(OAUTH_CONNECTIONS.DISPLAY_NAME.name),
      createdByUserId = row.getUUID(OAUTH_CONNECTIONS.CREATED_BY_USER_ID.name),
      createdAt = row.getOffsetDateTime(OAUTH_CONNECTIONS.CREATED_AT.name),
      updatedAt = row.getOffsetDateTime(OAUTH_CONNECTIONS.UPDATED_AT.name),
      lastValidatedAt = row.getOffsetDateTime(OAUTH_CONNECTIONS.LAST_VALIDATED_AT.name),
      scopes = row.getJsonArray(OAUTH_CONNECTIONS.SCOPES.name),
      providerAccountId = row.getString(OAUTH_CONNECTIONS.PROVIDER_ACCOUNT_ID.name),
    )

  fun toRecord(payload: JsonObject) =
    OAuthConnectionsRecord().apply {
      payload.applyIfPresent(OAUTH_CONNECTIONS.ID, JsonObject::getUUID) { value -> id = value }
      payload.applyIfPresent(OAUTH_CONNECTIONS.VESSEL_ID, JsonObject::getUUID) { value -> vesselId = value }
      payload.applyIfPresent(OAUTH_CONNECTIONS.CHARTER_ID, JsonObject::getUUID) { value -> charterId = value }
      payload.applyIfPresent(OAUTH_CONNECTIONS.TYPE, JsonObject::getString) { value -> type = OAuthConnectionType.valueOf(value) }
      payload.applyIfPresent(OAUTH_CONNECTIONS.PROVIDER, JsonObject::getString) { value -> provider = OAuthProvider.valueOf(value) }
      payload.applyIfPresent(OAUTH_CONNECTIONS.STATUS, JsonObject::getString) { value -> status = OAuthConnectionStatus.valueOf(value) }
      payload.applyIfPresent(OAUTH_CONNECTIONS.DISPLAY_NAME, JsonObject::getString) { value -> displayName = value }
      payload.applyIfPresent(OAUTH_CONNECTIONS.CREATED_BY_USER_ID, JsonObject::getUUID) { value -> createdByUserId = value }
      payload.applyIfPresent(OAUTH_CONNECTIONS.CREATED_AT, JsonObject::getString) { value ->
        createdAt = OffsetDateTime.parse(value)
      }
      payload.applyIfPresent(OAUTH_CONNECTIONS.UPDATED_AT, JsonObject::getString) { value ->
        updatedAt = OffsetDateTime.parse(value)
      }
      payload.applyIfPresent(OAUTH_CONNECTIONS.LAST_VALIDATED_AT, JsonObject::getString) { value ->
        lastValidatedAt = OffsetDateTime.parse(value)
      }
      payload.applyIfPresent(OAUTH_CONNECTIONS.SCOPES, JsonObject::getJsonArray) { value -> scopes = value }
      payload.applyIfPresent(OAUTH_CONNECTIONS.PROVIDER_ACCOUNT_ID, JsonObject::getString) { value -> providerAccountId = value }
    }
}
