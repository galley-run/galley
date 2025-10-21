package run.galley.cloud.sql

import generated.jooq.tables.records.ChartersRecord
import generated.jooq.tables.references.CHARTERS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.andNotDeleted
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.applyPagination
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.toRecord
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import org.jooq.impl.DSL.currentOffsetDateTime
import run.galley.cloud.ApiStatus
import java.util.UUID

object CharterSql {
  fun listCharters(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CHARTERS)
      .applyConditions(*conditions)
      .andNotDeleted(CHARTERS.DELETED_AT)
      .applyPagination(request.pagination)
  }

  fun getCharter(request: EventBusQueryDataRequest): Query {
    val identifier = request.identifiers["id"]
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(CHARTERS)
      .where(CHARTERS.ID.eq(identifier?.toUUID()))
      .applyConditions(requiredConditions = listOf(CHARTERS.VESSEL_ID), *conditions)
      .andNotDeleted(CHARTERS.DELETED_AT)
  }

  fun createCharter(request: EventBusCmdDataRequest): Query {
    val payload = request.payload ?: throw ApiStatus.REQUEST_BODY_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .insertInto(CHARTERS)
      .set(
        mapOf(
          CHARTERS.NAME to payload.getString(CHARTERS.NAME.name),
          CHARTERS.DESCRIPTION to payload.getString(CHARTERS.DESCRIPTION.name),
          CHARTERS.VESSEL_ID to payload.getUUID(CHARTERS.VESSEL_ID.name),
          CHARTERS.USER_ID to userId,
        ),
      ).returning()
  }

  fun patchCharter(request: EventBusCmdDataRequest): Query {
    val payload = request.payload ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier

    return Jooq.postgres
      .update(CHARTERS)
      .set(
        payload.toRecord<ChartersRecord>(CHARTERS),
      ).where(CHARTERS.ID.eq(identifier))
      .applyConditions(requiredConditions = listOf(CHARTERS.VESSEL_ID), *buildConditions(request.filters))
      .andNotDeleted(CHARTERS.DELETED_AT)
      .returning()
  }

  fun archiveCharter(request: EventBusCmdDataRequest): Query =
    Jooq.postgres
      .update(CHARTERS)
      .set(CHARTERS.DELETED_AT, currentOffsetDateTime())
      .where(CHARTERS.ID.eq(request.identifier))
      .applyConditions(requiredConditions = listOf(CHARTERS.VESSEL_ID), *buildConditions(request.filters))
      .andNotDeleted(CHARTERS.DELETED_AT)

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          CHARTERS.USER_ID.name -> CHARTERS.USER_ID.`in`(values.map { UUID.fromString(it) })
          CHARTERS.VESSEL_ID.name -> CHARTERS.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
