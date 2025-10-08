package run.galley.cloud.model

import generated.jooq.tables.pojos.Users
import generated.jooq.tables.references.USERS
import io.vertx.sqlclient.Row

object User {
  fun from(row: Row) =
    Users(
      id = row.getUUID(USERS.ID.name),
      email = row.getString(USERS.EMAIL.name),
      firstName = row.getString(USERS.FIRST_NAME.name),
      lastName = row.getString(USERS.LAST_NAME.name),
    )
}
