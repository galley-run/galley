package run.galley.cloud.sql

import generated.jooq.tables.references.CHARTER_PROJECTS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.andNotDeleted
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.applyPagination
import org.jooq.Condition
import org.jooq.Query
import java.util.UUID

object ProjectSql {
  fun listProjects(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CHARTER_PROJECTS)
      .applyConditions(*conditions)
      .andNotDeleted(CHARTER_PROJECTS.DELETED_AT)
      .applyPagination(request.pagination)
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          CHARTER_PROJECTS.ID.name -> CHARTER_PROJECTS.ID.`in`(values.map { UUID.fromString(it) })
          CHARTER_PROJECTS.VESSEL_ID.name -> CHARTER_PROJECTS.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          CHARTER_PROJECTS.CHARTER_ID.name -> CHARTER_PROJECTS.CHARTER_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
