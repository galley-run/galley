package run.galley.cloud.sql

import generated.jooq.enums.EngineMode
import generated.jooq.tables.references.VESSEL_ENGINES
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import run.galley.cloud.ApiStatus
import run.galley.cloud.model.factory.VesselEngineFactory
import java.util.UUID

object VesselEngineSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(VESSEL_ENGINES)
      .set(
        mapOf(
          VESSEL_ENGINES.NAME.name to payload.getString(VESSEL_ENGINES.NAME.name),
          VESSEL_ENGINES.VESSEL_ID.name to payload.getUUID(VESSEL_ENGINES.VESSEL_ID.name),
          VESSEL_ENGINES.MODE.name to EngineMode.controlled_engine,
        ),
      ).returning()
  }

  fun patch(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val identifier = request.identifier

    return Jooq.postgres
      .update(VESSEL_ENGINES)
      .set(
        VesselEngineFactory.toRecord(payload),
      ).where(VESSEL_ENGINES.ID.eq(identifier))
      .returning()
  }

  fun getByVesselId(request: EventBusQueryDataRequest): Query {
    val conditions = buildConditions(request.filters)

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINES)
      .applyConditions(*conditions)
  }

  fun get(request: EventBusQueryDataRequest): Query {
    val identifier = request.identifiers[VESSEL_ENGINES.ID.name]?.toUUID() ?: throw ApiStatus.VESSEL_ENGINE_ID_INCORRECT

    return Jooq.postgres
      .selectFrom(VESSEL_ENGINES)
      .where(VESSEL_ENGINES.ID.eq(identifier))
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          VESSEL_ENGINES.VESSEL_ID.name -> VESSEL_ENGINES.VESSEL_ID.`in`(values.map { UUID.fromString(it) })
          else -> null
        }
      }.toTypedArray()
}
