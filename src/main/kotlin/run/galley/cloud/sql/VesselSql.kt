package run.galley.cloud.sql

import java.util.UUID
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusDataRequest
import nl.clicqo.api.SortDirection
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.applyIdentifier
import nl.clicqo.ext.applyPagination
import nl.clicqo.ext.applySorting
import org.jooq.Condition
import org.jooq.Query
import org.jooq.SortField
import run.galley.cloud.db.generated.tables.references.VESSELS

object VesselSql {
  fun listVessels(request: EventBusDataRequest): Query {
    val conditions = buildConditions(request.filters)
    val sortFields = request.sort.map { buildSortField(it) }

    val query = Jooq.postgres
      .selectFrom(VESSELS)
      .applyConditions(conditions)
      .applySorting(sortFields)
      .let { q ->
        request.pagination?.run {
          return@run q.applyPagination(this.offset, this.limit)
        }

        return@let q
      }

    return query
  }

  fun getVessel(request: EventBusDataRequest): Query {
    val vesselId = request.identifiers["vesselId"]
      ?: throw IllegalArgumentException("vesselId is required")

    return Jooq.postgres
      .selectFrom(VESSELS)
      .applyIdentifier(VESSELS.ID, UUID.fromString(vesselId))
  }

  private fun buildConditions(filters: Map<String, List<String>>): List<Condition> {
    return filters.mapNotNull { (field, values) ->
      when (field) {
        "id" -> VESSELS.ID.`in`(values.map { UUID.fromString(it) })
        "name" -> if (values.size == 1) VESSELS.NAME.containsIgnoreCase(values[0]) else null
        "userId" -> VESSELS.USER_ID.`in`(values.map { UUID.fromString(it) })
        else -> null
      }
    }
  }

  private fun buildSortField(sortField: nl.clicqo.api.SortField): SortField<*> {
    val field = when (sortField.field) {
      "id" -> VESSELS.ID
      "name" -> VESSELS.NAME
      "userId" -> VESSELS.USER_ID
      "createdAt" -> VESSELS.CREATED_AT
      else -> throw IllegalArgumentException("Unknown sort field: ${sortField.field}")
    }

    return when (sortField.direction) {
      SortDirection.ASC -> field.asc()
      SortDirection.DESC -> field.desc()
    }
  }
}
