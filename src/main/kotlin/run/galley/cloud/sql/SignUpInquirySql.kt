package run.galley.cloud.sql

import generated.jooq.enums.SignUpIntent
import generated.jooq.enums.TechnicalExperience
import generated.jooq.tables.references.SIGN_UP_INQUIRIES
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import org.jooq.Query
import run.galley.cloud.ApiStatus

object SignUpInquirySql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .insertInto(SIGN_UP_INQUIRIES)
      .set(
        mapOf(
          SIGN_UP_INQUIRIES.USER_ID to userId,
          SIGN_UP_INQUIRIES.VESSEL_ID to payload.getUUID(SIGN_UP_INQUIRIES.VESSEL_ID.name),
          SIGN_UP_INQUIRIES.INTENT to SignUpIntent.valueOf(payload.getString(SIGN_UP_INQUIRIES.INTENT.name)),
          SIGN_UP_INQUIRIES.TECHNICAL_EXPERIENCE to
            TechnicalExperience.valueOf(payload.getString(SIGN_UP_INQUIRIES.TECHNICAL_EXPERIENCE.name)),
          SIGN_UP_INQUIRIES.QUESTIONS to payload.getJsonObject(SIGN_UP_INQUIRIES.QUESTIONS.name),
        ),
      ).returning()
  }
}
