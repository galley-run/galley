package run.galley.cloud.sql

import generated.jooq.enums.NodeDeployMode
import generated.jooq.enums.NodeType
import generated.jooq.tables.references.VESSEL_ENGINE_NODES
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

object VesselEngineNodeSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(VESSEL_ENGINE_NODES)
      .set(
        mapOf(
          VESSEL_ENGINE_NODES.NAME to payload.getString(VESSEL_ENGINE_NODES.NAME.name),
          VESSEL_ENGINE_NODES.VESSEL_ID to payload.getUUID(VESSEL_ENGINE_NODES.VESSEL_ID.name),
          VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID to payload.getUUID(VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID.name),
          VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID to payload.getUUID(VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID.name),
          VESSEL_ENGINE_NODES.NODE_TYPE to
            payload
              .getString(VESSEL_ENGINE_NODES.NODE_TYPE.name)
              ?.let(NodeType::valueOf),
          VESSEL_ENGINE_NODES.DEPLOY_MODE to
            payload
              .getString(VESSEL_ENGINE_NODES.DEPLOY_MODE.name)
              ?.let(NodeDeployMode::valueOf),
          VESSEL_ENGINE_NODES.IP_ADDRESS to payload.getString(VESSEL_ENGINE_NODES.IP_ADDRESS.name),
          VESSEL_ENGINE_NODES.CPU to payload.getString(VESSEL_ENGINE_NODES.CPU.name),
          VESSEL_ENGINE_NODES.MEMORY to payload.getString(VESSEL_ENGINE_NODES.MEMORY.name),
          VESSEL_ENGINE_NODES.STORAGE to payload.getString(VESSEL_ENGINE_NODES.STORAGE.name),
        ),
      ).returning()
  }

  fun getByVesselId(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINE_NODES)
      .applyConditions(*conditions)
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          VESSEL_ENGINE_NODES.VESSEL_ID.name -> VESSEL_ENGINE_NODES.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
