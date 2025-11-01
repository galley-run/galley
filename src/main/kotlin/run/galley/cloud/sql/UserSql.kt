package run.galley.cloud.sql

import generated.jooq.tables.references.USERS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.eventbus.EventBusQueryDataRequest
import nl.clicqo.ext.toUUID
import org.jooq.Query
import run.galley.cloud.ApiStatus

object UserSql {
  fun getUser(request: EventBusQueryDataRequest): Query {
    val id = request.identifiers["id"]?.toUUID() ?: throw ApiStatus.ID_MISSING

    return Jooq.postgres
      .selectFrom(USERS)
      .where(USERS.ID.eq(id))
  }

  fun getUserByEmail(request: EventBusQueryDataRequest): Query {
    val email = request.filters["email"]?.firstOrNull() ?: throw ApiStatus.FILTER_MISSING

    return Jooq.postgres
      .selectFrom(USERS)
      .where(USERS.EMAIL.eq(email))
  }

  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload ?: throw ApiStatus.REQUEST_BODY_MISSING
    return Jooq.postgres
      .insertInto(USERS)
      .set(
        mapOf(
          USERS.EMAIL to payload.getString(USERS.EMAIL.name),
          USERS.FIRST_NAME to payload.getString(USERS.FIRST_NAME.name),
          USERS.LAST_NAME to payload.getString(USERS.LAST_NAME.name),
        ),
      ).returning()
  }
}
