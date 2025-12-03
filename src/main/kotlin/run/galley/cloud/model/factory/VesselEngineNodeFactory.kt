package run.galley.cloud.model.factory

import generated.jooq.enums.NodeDeployMode
import generated.jooq.enums.NodeProvisioningStatus
import generated.jooq.enums.NodeType
import generated.jooq.tables.pojos.VesselEngineNodes
import generated.jooq.tables.records.VesselEngineNodesRecord
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import nl.clicqo.ext.getUUID

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
      provisioningStatus =
        row
          .getString(VESSEL_ENGINE_NODES.PROVISIONING_STATUS.name)
          ?.let(NodeProvisioningStatus::valueOf),
      osMetadata = row.getJsonObject(VESSEL_ENGINE_NODES.OS_METADATA.name),
    )

  fun toRecord(payload: JsonObject) =
    VesselEngineNodesRecord().apply {
      payload.applyIfPresent(VESSEL_ENGINE_NODES.NAME, JsonObject::getString) { value -> name = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.VESSEL_ID, JsonObject::getUUID) { value -> vesselId = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID, JsonObject::getUUID) { value -> vesselEngineId = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID, JsonObject::getUUID) { value -> vesselEngineRegionId = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.NODE_TYPE, JsonObject::getString) { value -> nodeType = NodeType.valueOf(value) }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.DEPLOY_MODE, JsonObject::getString) { value ->
        deployMode =
          value?.let(NodeDeployMode::valueOf)
      }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.IP_ADDRESS, JsonObject::getString) { value -> ipAddress = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.CPU, JsonObject::getString) { value -> cpu = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.MEMORY, JsonObject::getString) { value -> memory = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.STORAGE, JsonObject::getString) { value -> storage = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.OS_METADATA, JsonObject::getJsonObject) { value -> osMetadata = value }
      payload.applyIfPresent(VESSEL_ENGINE_NODES.PROVISIONING_STATUS, JsonObject::getString) { value ->
        provisioningStatus = NodeProvisioningStatus.valueOf(value)
      }
    }
}
