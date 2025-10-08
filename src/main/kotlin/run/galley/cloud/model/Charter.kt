package run.galley.cloud.model

import generated.jooq.tables.pojos.Charters
import generated.jooq.tables.references.CHARTERS
import io.vertx.sqlclient.Row

object Charter {
  fun from(row: Row) =
    Charters(
      id = row.getUUID(CHARTERS.ID.name),
      vesselId = row.getUUID(CHARTERS.VESSEL_ID.name),
      name = row.getString(CHARTERS.NAME.name),
      description = row.getString(CHARTERS.DESCRIPTION.name),
    )
}
