package run.galley.cloud.sql

import generated.jooq.tables.references.VESSELS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.ext.keysToSnakeCase
import org.jooq.Query
import run.galley.cloud.ApiStatus

object VesselSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .insertInto(VESSELS)
      .set(
        mapOf(
          VESSELS.NAME.name to payload.getString(VESSELS.NAME.name),
          VESSELS.USER_ID.name to userId,
        ),
      ).returning()
  }
}
