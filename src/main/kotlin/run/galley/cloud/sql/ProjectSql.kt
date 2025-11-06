package run.galley.cloud.sql

import generated.jooq.tables.records.CharterProjectsRecord
import generated.jooq.tables.references.CHARTER_PROJECTS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.andNotDeleted
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.applyPagination
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toRecord
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import org.jooq.impl.DSL.currentOffsetDateTime
import run.galley.cloud.ApiStatus
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

  fun getProject(request: EventBusQueryDataRequest): Query {
    val identifier = request.identifiers["id"]
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(CHARTER_PROJECTS)
      .where(CHARTER_PROJECTS.ID.eq(identifier?.toUUID()))
      .applyConditions(requiredConditions = listOf(CHARTER_PROJECTS.VESSEL_ID, CHARTER_PROJECTS.CHARTER_ID), *conditions)
      .andNotDeleted(CHARTER_PROJECTS.DELETED_AT)
  }

  fun createProject(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(CHARTER_PROJECTS)
      .set(
        mapOf(
          CHARTER_PROJECTS.VESSEL_ID to payload.getUUID(CHARTER_PROJECTS.VESSEL_ID.name),
          CHARTER_PROJECTS.CHARTER_ID to payload.getUUID(CHARTER_PROJECTS.CHARTER_ID.name),
          CHARTER_PROJECTS.NAME to payload.getString(CHARTER_PROJECTS.NAME.name),
          CHARTER_PROJECTS.ENVIRONMENT to payload.getString(CHARTER_PROJECTS.ENVIRONMENT.name),
          CHARTER_PROJECTS.PURPOSE to payload.getString(CHARTER_PROJECTS.PURPOSE.name),
        ),
      ).returning()
  }

  fun patchProject(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier

    return Jooq.postgres
      .update(CHARTER_PROJECTS)
      .set(
        payload.toRecord<CharterProjectsRecord>(CHARTER_PROJECTS),
      ).where(CHARTER_PROJECTS.ID.eq(identifier))
      .applyConditions(
        requiredConditions = listOf(CHARTER_PROJECTS.VESSEL_ID, CHARTER_PROJECTS.CHARTER_ID),
        *buildConditions(request.filters),
      ).andNotDeleted(CHARTER_PROJECTS.DELETED_AT)
      .returning()
  }

  fun archiveProject(request: EventBusCmdDataRequest): Query =
    Jooq.postgres
      .update(CHARTER_PROJECTS)
      .set(CHARTER_PROJECTS.DELETED_AT, currentOffsetDateTime())
      .where(CHARTER_PROJECTS.ID.eq(request.identifier))
      .applyConditions(
        requiredConditions = listOf(CHARTER_PROJECTS.VESSEL_ID, CHARTER_PROJECTS.CHARTER_ID),
        *buildConditions(request.filters),
      ).andNotDeleted(CHARTER_PROJECTS.DELETED_AT)

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
