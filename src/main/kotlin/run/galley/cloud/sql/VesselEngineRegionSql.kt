package run.galley.cloud.sql

import generated.jooq.tables.references.VESSEL_ENGINE_REGIONS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselEngineRegionFactory
import java.util.UUID

object VesselEngineRegionSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(VESSEL_ENGINE_REGIONS)
      .set(VesselEngineRegionFactory.toRecord(payload))
      .returning()
  }

  fun getByVesselId(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINE_REGIONS)
      .applyConditions(*conditions)
  }

  fun get(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    val id = request.identifiers["id"]?.toUUID() ?: throw ApiStatus.ID_MISSING

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINE_REGIONS)
      .where(VESSEL_ENGINE_REGIONS.ID.eq(id))
      .applyConditions(*conditions)
  }

  fun patch(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .update(VESSEL_ENGINE_REGIONS)
      .set(VesselEngineRegionFactory.toRecord(payload))
      .where(VESSEL_ENGINE_REGIONS.ID.eq(identifier))
      .applyConditions(*conditions)
      .returning()
  }

  fun delete(request: EventBusCmdDataRequest): Query {
    val identifier = request.identifier
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .deleteFrom(VESSEL_ENGINE_REGIONS)
      .where(VESSEL_ENGINE_REGIONS.ID.eq(identifier))
      .applyConditions(*conditions)
      .returning()
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          VESSEL_ENGINE_REGIONS.VESSEL_ID.name -> VESSEL_ENGINE_REGIONS.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
