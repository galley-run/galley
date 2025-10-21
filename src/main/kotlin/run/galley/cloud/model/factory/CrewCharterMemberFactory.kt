package run.galley.cloud.model.factory

import generated.jooq.enums.CharterRole
import generated.jooq.tables.pojos.CrewCharterMember
import generated.jooq.tables.references.CREW_CHARTER_MEMBER
import io.vertx.sqlclient.Row

object CrewCharterMemberFactory {
  fun from(row: Row): CrewCharterMember =
    CrewCharterMember(
      id = row.getUUID(CREW_CHARTER_MEMBER.ID.name),
      crewId = row.getUUID(CREW_CHARTER_MEMBER.CREW_ID.name),
      charterId = row.getUUID(CREW_CHARTER_MEMBER.CHARTER_ID.name),
      charterRole = row.getString(CREW_CHARTER_MEMBER.CHARTER_ROLE.name)?.let(CharterRole::valueOf),
      createdAt = row.getOffsetDateTime(CREW_CHARTER_MEMBER.CREATED_AT.name),
      deletedAt = row.getOffsetDateTime(CREW_CHARTER_MEMBER.DELETED_AT.name),
    )
}
