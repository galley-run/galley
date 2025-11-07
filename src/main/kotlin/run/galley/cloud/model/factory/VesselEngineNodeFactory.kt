package run.galley.cloud.model.factory

import generated.jooq.enums.NodeDeployMode
import generated.jooq.enums.NodeType
import generated.jooq.tables.pojos.VesselEngineNodes
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import io.vertx.sqlclient.Row

object VesselEngineNodeFactory {
  fun from(row: Row) =
    VesselEngineNodes(
      id = row.getUUID(VESSEL_ENGINE_NODES.ID.name),
      vesselId = row.getUUID(VESSEL_ENGINE_NODES.VESSEL_ID.name),
      vesselEngineId = row.getUUID(VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID.name),
      vesselEngineRegionId = row.getUUID(VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID.name),
      nodeType = row.getString(VESSEL_ENGINE_NODES.NODE_TYPE.name)?.let(NodeType::valueOf),
      deployMode = row.getString(VESSEL_ENGINE_NODES.DEPLOY_MODE.name)?.let(NodeDeployMode::valueOf),
      name = row.getString(VESSEL_ENGINE_NODES.NAME.name),
      ipAddress = row.getString(VESSEL_ENGINE_NODES.IP_ADDRESS.name),
      cpu = row.getString(VESSEL_ENGINE_NODES.CPU.name),
      memory = row.getString(VESSEL_ENGINE_NODES.MEMORY.name),
      storage = row.getString(VESSEL_ENGINE_NODES.STORAGE.name),
      provisioning = row.getBoolean(VESSEL_ENGINE_NODES.PROVISIONING.name),
    )
}
