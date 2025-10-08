package run.galley.cloud.sql

import generated.jooq.tables.references.USERS
import generated.jooq.tables.references.VESSELS
import nl.clicqo.api.SortDirection
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import org.jooq.SortField
import run.galley.cloud.ApiStatus
import java.util.UUID

object UserSql {
  fun getUser(request: EventBusQueryDataRequest): Query {
    val id = request.identifiers["id"]?.toUUID() ?: throw ApiStatus.ID_MISSING

    return Jooq.postgres
      .selectFrom(USERS)
      .where(USERS.ID.eq(id))
  }

  fun getUserByEmail(request: EventBusQueryDataRequest): Query {
    val email = request.filters["email"]?.firstOrNull() ?: throw ApiStatus.FILTER_MISSING

    return Jooq.postgres
      .selectFrom(USERS)
      .where(USERS.EMAIL.eq(email))
  }

  private fun buildConditions(filters: Map<String, List<String>>): List<Condition> =
    filters.mapNotNull { (field, values) ->
      when (field) {
        "id" -> VESSELS.ID.`in`(values.map { UUID.fromString(it) })
        "name" -> if (values.size == 1) VESSELS.NAME.containsIgnoreCase(values[0]) else null
        "userId" -> VESSELS.USER_ID.`in`(values.map { UUID.fromString(it) })
        else -> null
      }
    }

  private fun buildSortField(sortField: nl.clicqo.api.SortField): SortField<*> {
    val field =
      when (sortField.field) {
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
