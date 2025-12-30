package run.galley.cloud.model.factory

import generated.jooq.enums.OAuthCredentialKind
import generated.jooq.tables.pojos.OAuthCredentials
import generated.jooq.tables.records.OAuthCredentialsRecord
import generated.jooq.tables.references.OAUTH_CREDENTIALS
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import nl.clicqo.ext.getUUID
import java.time.OffsetDateTime

object OAuthCredentialFactory {
  fun from(row: Row) =
    OAuthCredentials(
      id = row.getUUID(OAUTH_CREDENTIALS.ID.name),
      connectionId = row.getUUID(OAUTH_CREDENTIALS.CONNECTION_ID.name),
      credentialKind = row.getString(OAUTH_CREDENTIALS.CREDENTIAL_KIND.name)?.let(OAuthCredentialKind::valueOf),
      accessTokenEncrypted = row.getString(OAUTH_CREDENTIALS.ACCESS_TOKEN_ENCRYPTED.name),
      refreshTokenEncrypted = row.getString(OAUTH_CREDENTIALS.REFRESH_TOKEN_ENCRYPTED.name),
      expiresAt = row.getOffsetDateTime(OAUTH_CREDENTIALS.EXPIRES_AT.name),
      tokenType = row.getString(OAUTH_CREDENTIALS.TOKEN_TYPE.name),
      providerMetadata = row.getJsonObject(OAUTH_CREDENTIALS.PROVIDER_METADATA.name),
      installationId = row.getLong(OAUTH_CREDENTIALS.INSTALLATION_ID.name),
      appId = row.getLong(OAUTH_CREDENTIALS.APP_ID.name),
      accountLogin = row.getString(OAUTH_CREDENTIALS.ACCOUNT_LOGIN.name),
      accountType = row.getString(OAUTH_CREDENTIALS.ACCOUNT_TYPE.name),
      instanceUrl = row.getString(OAUTH_CREDENTIALS.INSTANCE_URL.name),
      createdAt = row.getOffsetDateTime(OAUTH_CREDENTIALS.CREATED_AT.name),
      updatedAt = row.getOffsetDateTime(OAUTH_CREDENTIALS.UPDATED_AT.name),
    )

  fun toRecord(payload: JsonObject) =
    OAuthCredentialsRecord().apply {
      payload.applyIfPresent(OAUTH_CREDENTIALS.ID, JsonObject::getUUID) { value -> id = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.CONNECTION_ID, JsonObject::getUUID) { value -> connectionId = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.CREDENTIAL_KIND, JsonObject::getString) { value ->
        credentialKind =
          OAuthCredentialKind.valueOf(value)
      }
      payload.applyIfPresent(
        OAUTH_CREDENTIALS.ACCESS_TOKEN_ENCRYPTED,
        JsonObject::getString,
      ) { value -> accessTokenEncrypted = value }
      payload.applyIfPresent(
        OAUTH_CREDENTIALS.REFRESH_TOKEN_ENCRYPTED,
        JsonObject::getString,
      ) { value -> refreshTokenEncrypted = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.EXPIRES_AT, JsonObject::getString) { value ->
        expiresAt = OffsetDateTime.parse(value)
      }
      payload.applyIfPresent(OAUTH_CREDENTIALS.TOKEN_TYPE, JsonObject::getString) { value -> tokenType = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.PROVIDER_METADATA, JsonObject::getJsonObject) { value ->
        providerMetadata = value
      }
      payload.applyIfPresent(OAUTH_CREDENTIALS.INSTALLATION_ID, JsonObject::getLong) { value -> installationId = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.APP_ID, JsonObject::getLong) { value -> appId = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.ACCOUNT_LOGIN, JsonObject::getString) { value -> accountLogin = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.ACCOUNT_TYPE, JsonObject::getString) { value -> accountType = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.INSTANCE_URL, JsonObject::getString) { value -> instanceUrl = value }
      payload.applyIfPresent(OAUTH_CREDENTIALS.CREATED_AT, JsonObject::getString) { value ->
        createdAt = OffsetDateTime.parse(value)
      }
      payload.applyIfPresent(OAUTH_CREDENTIALS.UPDATED_AT, JsonObject::getString) { value ->
        updatedAt = OffsetDateTime.parse(value)
      }
    }
}
