package run.galley.cloud.sql

import generated.jooq.enums.MemberStatus
import generated.jooq.tables.references.CREW
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import java.time.OffsetDateTime
import java.util.UUID

object CrewSql {
  fun getCrewMemberByUserAndVessel(request: EventBusQueryDataRequest): Query {
    val userId = request.identifiers["userId"] ?: throw ApiStatus.ID_MISSING
    val vesselId = request.identifiers["vesselId"] ?: throw ApiStatus.ID_MISSING
    return Jooq.postgres
      .selectFrom(CREW)
      .applyConditions(
        CREW.USER_ID.eq(userId.toUUID()),
        CREW.VESSEL_ID.eq(vesselId.toUUID()),
      ).and(CREW.DELETED_AT.isNull.or(CREW.DELETED_AT.gt(OffsetDateTime.now())))
  }

  fun listActive(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CREW)
      .applyConditions(*conditions)
      .and(CREW.STATUS.eq(MemberStatus.active))
      .and(CREW.DELETED_AT.isNull.or(CREW.DELETED_AT.gt(OffsetDateTime.now())))
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          "userId" -> CREW.USER_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
