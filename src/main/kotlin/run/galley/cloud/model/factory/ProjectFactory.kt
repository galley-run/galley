package run.galley.cloud.model.factory

import generated.jooq.tables.pojos.CharterProjects
import generated.jooq.tables.references.CHARTER_PROJECTS
import io.vertx.sqlclient.Row

object ProjectFactory {
  fun from(row: Row) =
    CharterProjects(
      id = row.getUUID(CHARTER_PROJECTS.ID.name),
      vesselId = row.getUUID(CHARTER_PROJECTS.VESSEL_ID.name),
      charterId = row.getUUID(CHARTER_PROJECTS.CHARTER_ID.name),
      name = row.getString(CHARTER_PROJECTS.NAME.name),
      environment = row.getString(CHARTER_PROJECTS.ENVIRONMENT.name),
      purpose = row.getString(CHARTER_PROJECTS.PURPOSE.name),
    )
}
