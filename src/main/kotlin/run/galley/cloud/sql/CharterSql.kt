package run.galley.cloud.sql

import generated.jooq.tables.references.CHARTERS
import generated.jooq.tables.references.CREW
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import java.time.OffsetDateTime
import java.util.UUID

object CharterSql {
  fun listCharters(request: EventBusDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CHARTERS)
      .applyConditions(*conditions)
      .and(CHARTERS.DELETED_AT.isNull.or(CHARTERS.DELETED_AT.gt(OffsetDateTime.now())))
  }

  fun getCharter(request: EventBusDataRequest): Query {
    val identifier = request.identifiers["id"]
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(CHARTERS)
      .where(CHARTERS.ID.eq(identifier?.toUUID()))
      .applyConditions(*conditions)
      .and(CHARTERS.DELETED_AT.isNull.or(CHARTERS.DELETED_AT.gt(OffsetDateTime.now())))
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
