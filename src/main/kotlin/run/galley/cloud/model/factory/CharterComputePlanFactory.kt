package run.galley.cloud.model.factory

import generated.jooq.enums.NodeDeployMode
import generated.jooq.enums.NodeProvisioningStatus
import generated.jooq.enums.NodeType
import generated.jooq.tables.pojos.CharterComputePlans
import generated.jooq.tables.records.CharterComputePlansRecord
import generated.jooq.tables.records.VesselEngineNodesRecord
import generated.jooq.tables.references.CHARTER_COMPUTE_PLANS
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import nl.clicqo.ext.applyIfPresent
import nl.clicqo.ext.getUUID
import java.time.OffsetDateTime

object CharterComputePlanFactory {
  fun from(row: Row) =
    CharterComputePlans(
      id = row.getUUID(CHARTER_COMPUTE_PLANS.ID.name),
      vesselId = row.getUUID(CHARTER_COMPUTE_PLANS.VESSEL_ID.name),
      charterId = row.getUUID(CHARTER_COMPUTE_PLANS.CHARTER_ID.name),
      name = row.getString(CHARTER_COMPUTE_PLANS.NAME.name),
      application = row.getString(CHARTER_COMPUTE_PLANS.APPLICATION.name),
      requestsCpu = row.getString(CHARTER_COMPUTE_PLANS.REQUESTS_CPU.name),
      requestsMemory = row.getString(CHARTER_COMPUTE_PLANS.REQUESTS_MEMORY.name),
      limitsCpu = row.getString(CHARTER_COMPUTE_PLANS.LIMITS_CPU.name),
      limitsMemory = row.getString(CHARTER_COMPUTE_PLANS.LIMITS_MEMORY.name),
      billingEnabled = row.getBoolean(CHARTER_COMPUTE_PLANS.BILLING_ENABLED.name),
      billingPeriod = row.getString(CHARTER_COMPUTE_PLANS.BILLING_PERIOD.name),
      billingUnitPrice = row.getString(CHARTER_COMPUTE_PLANS.BILLING_UNIT_PRICE.name),
      createdAt = row.getOffsetDateTime(CHARTER_COMPUTE_PLANS.CREATED_AT.name),
      deletedAt = row.getOffsetDateTime(CHARTER_COMPUTE_PLANS.DELETED_AT.name),
    )

  fun toRecord(payload: JsonObject) =
    CharterComputePlansRecord().apply {
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.NAME, JsonObject::getString) { value -> name = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.VESSEL_ID, JsonObject::getUUID) { value -> vesselId = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.CHARTER_ID, JsonObject::getUUID) { value -> charterId = value }

      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.NAME, JsonObject::getString) { value -> name = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.APPLICATION, JsonObject::getString) { value -> application = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.REQUESTS_CPU, JsonObject::getString) { value -> requestsCpu = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.REQUESTS_MEMORY, JsonObject::getString) { value -> requestsMemory = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.LIMITS_CPU, JsonObject::getString) { value -> limitsCpu = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.LIMITS_MEMORY, JsonObject::getString) { value -> limitsMemory = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.BILLING_ENABLED, JsonObject::getBoolean) { value -> billingEnabled = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.BILLING_PERIOD, JsonObject::getString) { value -> billingPeriod = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.BILLING_UNIT_PRICE, JsonObject::getString) { value -> billingUnitPrice = value }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.CREATED_AT, JsonObject::getString) { value -> createdAt = OffsetDateTime.parse(value) }
      payload.applyIfPresent(CHARTER_COMPUTE_PLANS.DELETED_AT, JsonObject::getString) { value -> deletedAt = OffsetDateTime.parse(value) }
    }
}
