package run.galley.cloud.sql

import generated.jooq.tables.references.VESSEL_ENGINE_NODES
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselEngineNodeFactory
import java.util.UUID

object VesselEngineNodeSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(VESSEL_ENGINE_NODES)
      .set(VesselEngineNodeFactory.toRecord(payload))
      .returning()
  }

  fun patch(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .update(VESSEL_ENGINE_NODES)
      .set(VesselEngineNodeFactory.toRecord(payload))
      .where(VESSEL_ENGINE_NODES.ID.eq(identifier))
      .applyConditions(*conditions)
      .returning()
  }

  fun delete(request: EventBusCmdDataRequest): Query {
    val identifier = request.identifier
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .deleteFrom(VESSEL_ENGINE_NODES)
      .where(VESSEL_ENGINE_NODES.ID.eq(identifier))
      .applyConditions(*conditions)
      .returning()
  }

  fun find(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINE_NODES)
      .applyConditions(*conditions)
  }

  fun get(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)
    val id = request.identifiers["id"]?.toUUID() ?: throw ApiStatus.ID_MISSING

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINE_NODES)
      .where(VESSEL_ENGINE_NODES.ID.eq(id))
      .applyConditions(*conditions)
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          VESSEL_ENGINE_NODES.VESSEL_ID.name -> {
            VESSEL_ENGINE_NODES.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          }

          VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID.name -> {
            VESSEL_ENGINE_NODES.VESSEL_ENGINE_REGION_ID.`in`(
              values.map { UUID.fromString(it) },
            )
          }

          VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID.name -> {
            VESSEL_ENGINE_NODES.VESSEL_ENGINE_ID.`in`(values.map { UUID.fromString(it) })
          }

          VESSEL_ENGINE_NODES.PROVISIONING_STATUS.name -> {
            VESSEL_ENGINE_NODES.PROVISIONING_STATUS.`in`(values)
          }

          VESSEL_ENGINE_NODES.IP_ADDRESS.name -> {
            VESSEL_ENGINE_NODES.IP_ADDRESS.eq(values.first())
          }

          VESSEL_ENGINE_NODES.NAME.name -> {
            VESSEL_ENGINE_NODES.NAME.eq(values.first())
          }

          else -> {
            null
          }
        }
      }.toTypedArray()
}
