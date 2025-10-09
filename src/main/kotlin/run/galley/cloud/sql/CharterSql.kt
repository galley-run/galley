package run.galley.cloud.sql

import generated.jooq.tables.pojos.Charters
import generated.jooq.tables.references.CHARTERS
import generated.jooq.tables.references.CREW
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import java.time.OffsetDateTime
import java.util.UUID

object CharterSql {
  fun listCharters(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CHARTERS)
      .applyConditions(*conditions)
      .and(CHARTERS.DELETED_AT.isNull.or(CHARTERS.DELETED_AT.gt(OffsetDateTime.now())))
  }

  fun getCharter(request: EventBusQueryDataRequest): Query {
    val identifier = request.identifiers["id"]
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(CHARTERS)
      .where(CHARTERS.ID.eq(identifier?.toUUID()))
      .applyConditions(*conditions)
      .and(CHARTERS.DELETED_AT.isNull.or(CHARTERS.DELETED_AT.gt(OffsetDateTime.now())))
  }

  fun createCharter(request: EventBusCmdDataRequest<Charters>): Query {
    val payload = request.payload?.toOne() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .insertInto(CHARTERS)
      .set(
        mapOf(
          CHARTERS.NAME to payload.name,
          CHARTERS.DESCRIPTION to payload.description,
          CHARTERS.VESSEL_ID to payload.vesselId,
          CHARTERS.USER_ID to userId,
        ),
      ).returning()
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
