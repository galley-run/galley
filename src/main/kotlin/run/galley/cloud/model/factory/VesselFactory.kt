package run.galley.cloud.model.factory

import generated.jooq.tables.pojos.Vessels
import generated.jooq.tables.references.VESSELS
import io.vertx.sqlclient.Row

object VesselFactory {
  fun from(row: Row) =
    Vessels(
      id = row.getUUID(VESSELS.ID.name),
      name = row.getString(VESSELS.NAME.name),
    )
}
