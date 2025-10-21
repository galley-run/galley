package run.galley.cloud.sql

import generated.jooq.enums.MemberStatus
import generated.jooq.tables.references.CREW
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.andActivated
import nl.clicqo.ext.andNotDeleted
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query

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

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          "userId" -> CREW.USER_ID.`in`(values.map { it.toUUID() })
          else -> null
        }
      }.toTypedArray()
}
