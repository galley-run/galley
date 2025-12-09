package run.galley.cloud.sql

import generated.jooq.tables.records.CharterComputePlansRecord
import generated.jooq.tables.references.CHARTER_COMPUTE_PLANS
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
import run.galley.cloud.model.factory.CharterComputePlanFactory
import java.util.UUID

object CharterComputePlanSql {
  fun listComputePlans(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    return Jooq.postgres
      .selectFrom(CHARTER_COMPUTE_PLANS)
      .applyConditions(*conditions)
      .andNotDeleted(CHARTER_COMPUTE_PLANS.DELETED_AT)
      .applyPagination(request.pagination)
  }

  fun getComputePlan(request: EventBusQueryDataRequest): Query {
    val identifier = request.identifiers["id"]
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(CHARTER_COMPUTE_PLANS)
      .where(CHARTER_COMPUTE_PLANS.ID.eq(identifier?.toUUID()))
      .applyConditions(requiredConditions = listOf(CHARTER_COMPUTE_PLANS.VESSEL_ID, CHARTER_COMPUTE_PLANS.CHARTER_ID), *conditions)
      .andNotDeleted(CHARTER_COMPUTE_PLANS.DELETED_AT)
  }

  fun createComputePlan(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(CHARTER_COMPUTE_PLANS)
      .set(CharterComputePlanFactory.toRecord(payload))
      .returning()
  }

  fun createComputePlans(computePlans: List<CharterComputePlansRecord>): Query =
    Jooq.postgres.insertInto(CHARTER_COMPUTE_PLANS).set(computePlans)

  fun patchComputePlan(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier

    return Jooq.postgres
      .update(CHARTER_COMPUTE_PLANS)
      .set(CharterComputePlanFactory.toRecord(payload))
      .where(CHARTER_COMPUTE_PLANS.ID.eq(identifier))
      .applyConditions(
        requiredConditions = listOf(CHARTER_COMPUTE_PLANS.VESSEL_ID, CHARTER_COMPUTE_PLANS.CHARTER_ID),
        *buildConditions(request.filters),
      ).andNotDeleted(CHARTER_COMPUTE_PLANS.DELETED_AT)
      .returning()
  }

  fun archiveComputePlan(request: EventBusCmdDataRequest): Query =
    Jooq.postgres
      .update(CHARTER_COMPUTE_PLANS)
      .set(CHARTER_COMPUTE_PLANS.DELETED_AT, currentOffsetDateTime())
      .where(CHARTER_COMPUTE_PLANS.ID.eq(request.identifier))
      .applyConditions(
        requiredConditions = listOf(CHARTER_COMPUTE_PLANS.VESSEL_ID, CHARTER_COMPUTE_PLANS.CHARTER_ID),
        *buildConditions(request.filters),
      ).andNotDeleted(CHARTER_COMPUTE_PLANS.DELETED_AT)

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          CHARTER_COMPUTE_PLANS.ID.name -> CHARTER_COMPUTE_PLANS.ID.`in`(values.map { UUID.fromString(it) })
          CHARTER_COMPUTE_PLANS.VESSEL_ID.name -> CHARTER_COMPUTE_PLANS.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          CHARTER_COMPUTE_PLANS.CHARTER_ID.name -> CHARTER_COMPUTE_PLANS.CHARTER_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
