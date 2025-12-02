package run.galley.cloud.model.factory

import generated.jooq.enums.AgentConnectionStatus
import generated.jooq.enums.EngineMode
import generated.jooq.tables.pojos.VesselEngines
import generated.jooq.tables.records.VesselEnginesRecord
import generated.jooq.tables.references.VESSEL_ENGINES
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import java.time.OffsetDateTime

object VesselEngineFactory {
  fun from(row: Row) =
    VesselEngines(
      id = row.getUUID(VESSEL_ENGINES.ID.name),
      vesselId = row.getUUID(VESSEL_ENGINES.VESSEL_ID.name),
      name = row.getString(VESSEL_ENGINES.NAME.name),
      mode = row.getString(VESSEL_ENGINES.MODE.name)?.let(EngineMode::valueOf),
      agentConnectionStatus =
        row
          .getString(VESSEL_ENGINES.AGENT_CONNECTION_STATUS.name)
          ?.let(AgentConnectionStatus::valueOf),
      lastConnectionError = row.getString(VESSEL_ENGINES.LAST_CONNECTION_ERROR.name),
      lastAgentConnectionAt = row.getOffsetDateTime(VESSEL_ENGINES.LAST_AGENT_CONNECTION_AT.name),
    )

  fun toRecord(payload: JsonObject) =
    VesselEnginesRecord().apply {
      payload.applyIfPresent(VESSEL_ENGINES.NAME, JsonObject::getString) { value -> name = value }
      payload.applyIfPresent(VESSEL_ENGINES.AGENT_CONNECTION_STATUS, JsonObject::getString) { value ->
        agentConnectionStatus =
          AgentConnectionStatus.valueOf(value)
      }
      payload.applyIfPresent(VESSEL_ENGINES.MODE, JsonObject::getString) { value ->
        mode = EngineMode.valueOf(value)
      }
      payload.applyIfPresent(
        VESSEL_ENGINES.LAST_CONNECTION_ERROR,
        JsonObject::getString,
      ) { value -> lastConnectionError = value }
      payload.applyIfPresent(
        VESSEL_ENGINES.LAST_AGENT_CONNECTION_AT,
        JsonObject::getString,
      ) { value -> lastAgentConnectionAt = OffsetDateTime.parse(value) }
    }
}
