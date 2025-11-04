package run.galley.cloud.sql

import generated.jooq.tables.references.VESSEL_BILLING_PROFILE
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import org.jooq.Query
import run.galley.cloud.ApiStatus

object VesselBillingProfileSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING

    return Jooq.postgres
      .insertInto(VESSEL_BILLING_PROFILE)
      .set(
        mapOf(
          VESSEL_BILLING_PROFILE.VESSEL_ID to payload.getUUID(VESSEL_BILLING_PROFILE.VESSEL_ID.name),
          VESSEL_BILLING_PROFILE.COMPANY_NAME to payload.getString(VESSEL_BILLING_PROFILE.COMPANY_NAME.name),
          VESSEL_BILLING_PROFILE.BILLING_TO to payload.getString(VESSEL_BILLING_PROFILE.BILLING_TO.name),
          VESSEL_BILLING_PROFILE.ADDRESS1 to payload.getString(VESSEL_BILLING_PROFILE.ADDRESS1.name),
          VESSEL_BILLING_PROFILE.ADDRESS2 to payload.getString(VESSEL_BILLING_PROFILE.ADDRESS2.name),
          VESSEL_BILLING_PROFILE.POSTAL_CODE to payload.getString(VESSEL_BILLING_PROFILE.POSTAL_CODE.name),
          VESSEL_BILLING_PROFILE.CITY to payload.getString(VESSEL_BILLING_PROFILE.CITY.name),
          VESSEL_BILLING_PROFILE.STATE to payload.getString(VESSEL_BILLING_PROFILE.STATE.name),
          VESSEL_BILLING_PROFILE.COUNTRY to payload.getString(VESSEL_BILLING_PROFILE.COUNTRY.name),
          VESSEL_BILLING_PROFILE.EMAIL to payload.getString(VESSEL_BILLING_PROFILE.EMAIL.name),
          VESSEL_BILLING_PROFILE.PHONE to payload.getString(VESSEL_BILLING_PROFILE.PHONE.name),
          VESSEL_BILLING_PROFILE.VAT_NUMBER to payload.getString(VESSEL_BILLING_PROFILE.VAT_NUMBER.name),
        ),
      ).returning()
  }
}
