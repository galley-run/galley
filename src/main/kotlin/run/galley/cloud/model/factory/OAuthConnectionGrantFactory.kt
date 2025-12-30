package run.galley.cloud.model.factory

import generated.jooq.enums.OAuthGrantPermission
import generated.jooq.enums.OAuthGrantPrincipalType
import generated.jooq.tables.pojos.OAuthConnectionGrants
import generated.jooq.tables.records.OAuthConnectionGrantsRecord
import generated.jooq.tables.references.OAUTH_CONNECTION_GRANTS
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import nl.clicqo.ext.getUUID
import java.time.OffsetDateTime

object OAuthConnectionGrantFactory {
  fun from(row: Row) =
    OAuthConnectionGrants(
      id = row.getUUID(OAUTH_CONNECTION_GRANTS.ID.name),
      connectionId = row.getUUID(OAUTH_CONNECTION_GRANTS.CONNECTION_ID.name),
      principalType = row.getString(OAUTH_CONNECTION_GRANTS.PRINCIPAL_TYPE.name)?.let(OAuthGrantPrincipalType::valueOf),
      principalId = row.getString(OAUTH_CONNECTION_GRANTS.PRINCIPAL_ID.name),
      permission = row.getString(OAUTH_CONNECTION_GRANTS.PERMISSION.name)?.let(OAuthGrantPermission::valueOf),
      createdAt = row.getOffsetDateTime(OAUTH_CONNECTION_GRANTS.CREATED_AT.name),
    )

  fun toRecord(payload: JsonObject) =
    OAuthConnectionGrantsRecord().apply {
      payload.applyIfPresent(OAUTH_CONNECTION_GRANTS.ID, JsonObject::getUUID) { value -> id = value }
      payload.applyIfPresent(OAUTH_CONNECTION_GRANTS.CONNECTION_ID, JsonObject::getUUID) { value -> connectionId = value }
      payload.applyIfPresent(OAUTH_CONNECTION_GRANTS.PRINCIPAL_TYPE, JsonObject::getString) { value ->
        principalType =
          OAuthGrantPrincipalType.valueOf(value)
      }
      payload.applyIfPresent(OAUTH_CONNECTION_GRANTS.PRINCIPAL_ID, JsonObject::getString) { value -> principalId = value }
      payload.applyIfPresent(OAUTH_CONNECTION_GRANTS.PERMISSION, JsonObject::getString) { value ->
        permission =
          OAuthGrantPermission.valueOf(value)
      }
      payload.applyIfPresent(OAUTH_CONNECTION_GRANTS.CREATED_AT, JsonObject::getString) { value ->
        createdAt = OffsetDateTime.parse(value)
      }
    }
}
