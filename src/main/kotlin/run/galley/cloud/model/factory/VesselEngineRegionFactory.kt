package run.galley.cloud.model.factory

import generated.jooq.enums.GeoRegion
import generated.jooq.tables.pojos.VesselEngineRegions
import generated.jooq.tables.records.VesselEngineRegionsRecord
import generated.jooq.tables.references.VESSEL_ENGINE_REGIONS
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import nl.clicqo.ext.getUUID

object VesselEngineRegionFactory {
  fun from(row: Row) =
    VesselEngineRegions(
      id = row.getUUID(VESSEL_ENGINE_REGIONS.ID.name),
      vesselId = row.getUUID(VESSEL_ENGINE_REGIONS.VESSEL_ID.name),
      name = row.getString(VESSEL_ENGINE_REGIONS.NAME.name),
      providerName = row.getString(VESSEL_ENGINE_REGIONS.PROVIDER_NAME.name),
      geoRegion = row.getString(VESSEL_ENGINE_REGIONS.GEO_REGION.name)?.let(GeoRegion::valueOf),
      locationCity = row.getString(VESSEL_ENGINE_REGIONS.LOCATION_CITY.name),
      locationCountry = row.getString(VESSEL_ENGINE_REGIONS.LOCATION_COUNTRY.name),
    )

  fun toRecord(payload: JsonObject) =
    VesselEngineRegionsRecord().apply {
      payload.applyIfPresent(VESSEL_ENGINE_REGIONS.VESSEL_ID, JsonObject::getUUID) { value -> vesselId = value }
      payload.applyIfPresent(VESSEL_ENGINE_REGIONS.NAME, JsonObject::getString) { value -> name = value }
      payload.applyIfPresent(VESSEL_ENGINE_REGIONS.PROVIDER_NAME, JsonObject::getString) { value -> providerName = value }
      payload.applyIfPresent(VESSEL_ENGINE_REGIONS.GEO_REGION, JsonObject::getString) { value -> geoRegion = GeoRegion.valueOf(value) }
      payload.applyIfPresent(VESSEL_ENGINE_REGIONS.LOCATION_CITY, JsonObject::getString) { value -> locationCity = value }
      payload.applyIfPresent(VESSEL_ENGINE_REGIONS.LOCATION_COUNTRY, JsonObject::getString) { value -> locationCountry = value }
    }
}
