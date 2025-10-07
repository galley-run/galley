package run.galley.cloud.model

import io.vertx.sqlclient.Row
import run.galley.cloud.db.generated.tables.pojos.Users
import run.galley.cloud.db.generated.tables.references.USERS

object User {
  fun from(row: Row) = Users(
    id = row.getUUID(USERS.ID.name),
    email = row.getString(USERS.EMAIL.name),
    firstName = row.getString(USERS.FIRST_NAME.name),
    lastName = row.getString(USERS.LAST_NAME.name)
  )
}
