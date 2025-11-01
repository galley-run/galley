package run.galley.cloud.model.factory

import generated.jooq.tables.pojos.VesselBillingProfile
import generated.jooq.tables.references.VESSEL_BILLING_PROFILE
import io.vertx.sqlclient.Row

object VesselBillingProfileFactory {
  fun from(row: Row) =
    VesselBillingProfile(
      id = row.getUUID(VESSEL_BILLING_PROFILE.ID.name),
      vesselId = row.getUUID(VESSEL_BILLING_PROFILE.VESSEL_ID.name),
      companyName = row.getString(VESSEL_BILLING_PROFILE.COMPANY_NAME.name),
      billingTo = row.getString(VESSEL_BILLING_PROFILE.BILLING_TO.name),
      address1 = row.getString(VESSEL_BILLING_PROFILE.ADDRESS1.name),
      address2 = row.getString(VESSEL_BILLING_PROFILE.ADDRESS2.name),
      postalCode = row.getString(VESSEL_BILLING_PROFILE.POSTAL_CODE.name),
      city = row.getString(VESSEL_BILLING_PROFILE.CITY.name),
      state = row.getString(VESSEL_BILLING_PROFILE.STATE.name),
      country = row.getString(VESSEL_BILLING_PROFILE.COUNTRY.name),
      email = row.getString(VESSEL_BILLING_PROFILE.EMAIL.name),
      phone = row.getString(VESSEL_BILLING_PROFILE.PHONE.name),
      vatNumber = row.getString(VESSEL_BILLING_PROFILE.VAT_NUMBER.name),
    )
}
