package run.galley.cloud.model

import generated.jooq.tables.pojos.CharterComputePlans
import io.vertx.core.json.JsonObject

/**
 * Converts a CharterComputePlans POJO to JSON API resource object with nested structure
 * according to the OpenAPI specification.
 */
fun CharterComputePlans.toJsonAPIResourceObject(): JsonObject {
  val attributes =
    JsonObject()
      .put("vesselId", this.vesselId?.toString())
      .put("charterId", this.charterId?.toString())
      .put("name", this.name)
      .put("application", this.application)
      .put("createdAt", this.createdAt?.toString())

  // Build nested requests object
  val requests =
    JsonObject()
      .put("cpu", this.requestsCpu)
      .put("memory", this.requestsMemory)
  attributes.put("requests", requests)

  // Build nested limits object if any limit is set
  if (this.limitsCpu != null || this.limitsMemory != null) {
    val limits =
      JsonObject()
        .put("cpu", this.limitsCpu)
        .put("memory", this.limitsMemory)
    attributes.put("limits", limits)
  }

  // Build nested billing object
  val billing =
    JsonObject()
      .put("enabled", this.billingEnabled ?: false)
      .put("period", this.billingPeriod)
      .put("unitPrice", this.billingUnitPrice)
  attributes.put("billing", billing)

  return JsonObject()
    .put("type", "CharterComputePlan")
    .put("id", this.id?.toString())
    .put("attributes", attributes)
}
