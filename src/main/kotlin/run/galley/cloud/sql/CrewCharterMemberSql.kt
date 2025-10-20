package run.galley.cloud.sql

import generated.jooq.tables.references.CREW_CHARTER_MEMBER
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import java.time.OffsetDateTime

object CrewCharterMemberSql {
  fun listByCrewId(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CREW_CHARTER_MEMBER)
      .applyConditions(*conditions)
      .and(CREW_CHARTER_MEMBER.DELETED_AT.isNull.or(CREW_CHARTER_MEMBER.DELETED_AT.gt(OffsetDateTime.now()))) as Query
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          "crewId" -> CREW_CHARTER_MEMBER.CREW_ID.`in`(values.map { it.toUUID() })
          else -> null
        }
      }.toTypedArray()
}
