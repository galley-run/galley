package run.galley.cloud.model.factory

import generated.jooq.enums.GeoRegion
import generated.jooq.tables.pojos.VesselEngineRegions
import generated.jooq.tables.references.VESSEL_ENGINE_REGIONS
import io.vertx.sqlclient.Row

object VesselEngineRegionFactory {
  fun from(row: Row) =
    VesselEngineRegions(
      id = row.getUUID(VESSEL_ENGINE_REGIONS.ID.name),
      vesselId = row.getUUID(VESSEL_ENGINE_REGIONS.VESSEL_ID.name),
      vesselEngineId = row.getUUID(VESSEL_ENGINE_REGIONS.VESSEL_ENGINE_ID.name),
      name = row.getString(VESSEL_ENGINE_REGIONS.NAME.name),
      providerName = row.getString(VESSEL_ENGINE_REGIONS.PROVIDER_NAME.name),
      geoRegion = row.getString(VESSEL_ENGINE_REGIONS.GEO_REGION.name)?.let(GeoRegion::valueOf),
      locationCity = row.getString(VESSEL_ENGINE_REGIONS.LOCATION_CITY.name),
      locationCountry = row.getString(VESSEL_ENGINE_REGIONS.LOCATION_COUNTRY.name),
    )
}
