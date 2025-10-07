package run.galley.cloud.sql

import java.time.OffsetDateTime
import java.util.UUID
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.ext.applyConditions
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.db.generated.tables.references.CHARTERS
import run.galley.cloud.db.generated.tables.references.CREW

object CharterSql {
  fun listCharters(request: EventBusDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres.selectFrom(CHARTERS)
      .applyConditions(*conditions)
      .and(CHARTERS.DELETED_AT.isNull.or(CHARTERS.DELETED_AT.gt(OffsetDateTime.now())))
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> {
    return filters.mapNotNull { (field, values) ->
      when (field) {
        "userId" -> CREW.USER_ID.`in`(values.map { UUID.fromString(it) })
        else -> null
      }
    }.toTypedArray()
  }
}
