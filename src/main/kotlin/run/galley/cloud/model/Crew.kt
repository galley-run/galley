package run.galley.cloud.model

import io.vertx.sqlclient.Row
import run.galley.cloud.db.generated.enums.MemberStatus
import run.galley.cloud.db.generated.enums.VesselRole
import run.galley.cloud.db.generated.tables.pojos.Crew
import run.galley.cloud.db.generated.tables.references.CREW

object Crew {
  fun from(row: Row) = Crew(
    id = row.getUUID(CREW.ID.name),
    userId = row.getUUID(CREW.USER_ID.name),
    vesselId = row.getUUID(CREW.VESSEL_ID.name),
    vesselRole = VesselRole.valueOf(row.getString(CREW.VESSEL_ROLE.name)),
    status = MemberStatus.valueOf(row.getString(CREW.STATUS.name)),
    activatedAt = row.getOffsetDateTime(CREW.ACTIVATED_AT.name),
  )
}
