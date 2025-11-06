package run.galley.cloud.model.factory

import generated.jooq.enums.MemberStatus
import generated.jooq.enums.VesselRole
import generated.jooq.tables.pojos.Crew
import generated.jooq.tables.references.CREW
import io.vertx.sqlclient.Row

object CrewFactory {
  fun from(row: Row) =
    Crew(
      id = row.getUUID(CREW.ID.name),
      userId = row.getUUID(CREW.USER_ID.name),
      vesselId = row.getUUID(CREW.VESSEL_ID.name),
      vesselRole = VesselRole.valueOf(row.getString(CREW.VESSEL_ROLE.name)),
      status = MemberStatus.valueOf(row.getString(CREW.STATUS.name)),
      activationSalt = row.getString(CREW.ACTIVATION_SALT.name),
      activatedAt = row.getOffsetDateTime(CREW.ACTIVATED_AT.name),
    )
}
