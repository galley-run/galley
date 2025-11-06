package run.galley.cloud.sql

import generated.jooq.enums.MemberStatus
import generated.jooq.enums.VesselRole
import generated.jooq.tables.references.CREW
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.andActivated
import nl.clicqo.ext.andNotDeleted
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import java.time.OffsetDateTime

object CrewSql {
  fun listActive(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CREW)
      .applyConditions(*conditions)
      .and(CREW.STATUS.eq(MemberStatus.active))
      .andActivated(CREW.ACTIVATED_AT)
      .andNotDeleted(CREW.DELETED_AT)
  }

  fun list(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CREW)
      .applyConditions(*conditions)
      .andNotDeleted(CREW.DELETED_AT)
  }

  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .insertInto(CREW)
      .set(
        mapOf(
          CREW.USER_ID to userId,
          CREW.VESSEL_ID to payload.getUUID(CREW.VESSEL_ID.name),
          CREW.VESSEL_ROLE to VesselRole.valueOf(payload.getString(CREW.VESSEL_ROLE.name)),
          CREW.STATUS to MemberStatus.invited,
        ),
      ).returning()
  }

  fun activate(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .update(CREW)
      .set(
        mapOf(
          CREW.ACTIVATED_AT to OffsetDateTime.now(),
          CREW.ACTIVATION_SALT to "",
          CREW.STATUS to MemberStatus.active,
        ),
      ).applyConditions(*conditions)
      .and(CREW.STATUS.eq(MemberStatus.invited))
      .and(CREW.ACTIVATED_AT.isNull)
      .andNotDeleted(CREW.DELETED_AT)
      .returning()
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          CREW.USER_ID.name -> CREW.USER_ID.`in`(values.map { it.toUUID() })
          else -> null
        }
      }.toTypedArray()
}
