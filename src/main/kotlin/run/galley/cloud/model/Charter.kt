package run.galley.cloud.model

import io.vertx.sqlclient.Row
import run.galley.cloud.db.generated.tables.pojos.Charters
import run.galley.cloud.db.generated.tables.references.CHARTERS

object Charter {
  fun from(row: Row) = Charters(
    id = row.getUUID(CHARTERS.ID.name),
    vesselId = row.getUUID(CHARTERS.VESSEL_ID.name),
    name = row.getString(CHARTERS.NAME.name),
    description = row.getString(CHARTERS.DESCRIPTION.name),

  )
}
