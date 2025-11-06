package run.galley.cloud.model.factory

import generated.jooq.tables.pojos.Sessions
import generated.jooq.tables.references.SESSIONS
import io.vertx.sqlclient.Row

object SessionFactory {
  fun from(row: Row) =
    Sessions(
      id = row.getUUID(SESSIONS.ID.name),
      userId = row.getUUID(SESSIONS.USER_ID.name),
      deviceName = row.getString(SESSIONS.DEVICE_NAME.name),
      issuedAt = row.getOffsetDateTime(SESSIONS.ISSUED_AT.name),
      revokedAt = row.getOffsetDateTime(SESSIONS.REVOKED_AT.name),
      lastUsedAt = row.getOffsetDateTime(SESSIONS.LAST_USED_AT.name),
    )
}
