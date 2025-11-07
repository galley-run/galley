package run.galley.cloud.sql

import generated.jooq.enums.GeoRegion
import generated.jooq.tables.references.VESSEL_ENGINE_REGIONS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import java.util.UUID

object VesselEngineRegionSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(VESSEL_ENGINE_REGIONS)
      .set(
        mapOf(
          VESSEL_ENGINE_REGIONS.NAME.name to payload.getString(VESSEL_ENGINE_REGIONS.NAME.name),
          VESSEL_ENGINE_REGIONS.VESSEL_ID.name to payload.getUUID(VESSEL_ENGINE_REGIONS.VESSEL_ID.name),
          VESSEL_ENGINE_REGIONS.VESSEL_ENGINE_ID to payload.getUUID(VESSEL_ENGINE_REGIONS.VESSEL_ENGINE_ID.name),
          VESSEL_ENGINE_REGIONS.PROVIDER_NAME to payload.getString(VESSEL_ENGINE_REGIONS.PROVIDER_NAME.name),
          VESSEL_ENGINE_REGIONS.GEO_REGION to payload.getString(VESSEL_ENGINE_REGIONS.GEO_REGION.name)?.let(GeoRegion::valueOf),
          VESSEL_ENGINE_REGIONS.LOCATION_CITY to payload.getString(VESSEL_ENGINE_REGIONS.LOCATION_CITY.name),
          VESSEL_ENGINE_REGIONS.LOCATION_COUNTRY to payload.getString(VESSEL_ENGINE_REGIONS.LOCATION_COUNTRY.name),
        ),
      ).returning()
  }

  fun getByVesselId(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINE_REGIONS)
      .applyConditions(*conditions)
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
