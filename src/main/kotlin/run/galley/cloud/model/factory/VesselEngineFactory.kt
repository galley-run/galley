package run.galley.cloud.model.factory

import generated.jooq.enums.AgentConnectionStatus
import generated.jooq.enums.EngineMode
import generated.jooq.tables.pojos.VesselEngines
import generated.jooq.tables.references.VESSEL_ENGINES
import io.vertx.sqlclient.Row

object VesselEngineFactory {
  fun from(row: Row) =
    VesselEngines(
      id = row.getUUID(VESSEL_ENGINES.ID.name),
      vesselId = row.getUUID(VESSEL_ENGINES.VESSEL_ID.name),
      name = row.getString(VESSEL_ENGINES.NAME.name),
      mode = row.getString(VESSEL_ENGINES.MODE.name)?.let(EngineMode::valueOf),
      agentConnectionStatus = row.getString(VESSEL_ENGINES.AGENT_CONNECTION_STATUS.name)?.let(AgentConnectionStatus::valueOf),
      lastConnectionError = row.getString(VESSEL_ENGINES.LAST_CONNECTION_ERROR.name),
    )
}
