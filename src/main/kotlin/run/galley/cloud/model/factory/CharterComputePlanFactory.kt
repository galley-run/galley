package run.galley.cloud.model.factory

import generated.jooq.tables.pojos.CharterComputePlans
import generated.jooq.tables.references.CHARTER_COMPUTE_PLANS
import io.vertx.sqlclient.Row

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
}
