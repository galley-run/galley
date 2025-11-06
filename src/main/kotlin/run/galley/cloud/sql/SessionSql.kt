package run.galley.cloud.sql

import generated.jooq.tables.references.SESSIONS
import nl.clicqo.data.Jooq
import nl.clicqo.eventbus.EventBusCmdDataRequest
import nl.clicqo.ext.applyConditions
import nl.clicqo.ext.getUUID
import nl.clicqo.ext.keysToSnakeCase
import nl.clicqo.ext.toUUID
import org.jooq.Condition
import org.jooq.Query
import org.jooq.postgres.extensions.types.Inet
import run.galley.cloud.ApiStatus
import java.net.InetAddress
import java.time.OffsetDateTime

object SessionSql {
  fun create(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    val ip = payload.getString(SESSIONS.IP_ADDRESS.name)

    return Jooq.postgres
      .insertInto(SESSIONS)
      .set(
        mapOf(
          SESSIONS.USER_ID to userId,
          SESSIONS.REFRESH_TOKEN_HASH to payload.getString(SESSIONS.REFRESH_TOKEN_HASH.name),
          SESSIONS.DEVICE_NAME to payload.getString(SESSIONS.DEVICE_NAME.name),
          SESSIONS.USER_AGENT to payload.getString(SESSIONS.USER_AGENT.name),
          SESSIONS.IP_ADDRESS to Inet.inet(InetAddress.getByName(ip)),
          SESSIONS.EXPIRES_AT to OffsetDateTime.parse(payload.getString(SESSIONS.EXPIRES_AT.name)),
        ),
      ).returning()
  }

  fun get(request: EventBusCmdDataRequest): Query {
    val conditions = buildConditions(request.filters)
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .selectFrom(SESSIONS)
      .applyConditions(*conditions)
      .and(SESSIONS.USER_ID.eq(userId))
      .and(SESSIONS.REVOKED_AT.isNull)
      .and(SESSIONS.REPLACED_BY_ID.isNull)
      .and(SESSIONS.EXPIRES_AT.isNull.or(SESSIONS.EXPIRES_AT.ge(OffsetDateTime.now())))
  }

  fun update(request: EventBusCmdDataRequest): Query {
    val payload = request.payload?.keysToSnakeCase() ?: throw ApiStatus.REQUEST_BODY_MISSING
    val id = request.identifier ?: throw ApiStatus.ID_MISSING
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .update(SESSIONS)
      .set(
        mapOf(
          SESSIONS.REVOKED_AT to OffsetDateTime.now(),
          SESSIONS.REPLACED_BY_ID to payload.getUUID(SESSIONS.REPLACED_BY_ID.name),
        ),
      ).where(SESSIONS.ID.eq(id))
      .and(SESSIONS.USER_ID.eq(userId))
      .and(SESSIONS.REVOKED_AT.isNull)
      .and(SESSIONS.EXPIRES_AT.isNotNull.and(SESSIONS.EXPIRES_AT.ge(OffsetDateTime.now())))
      .returning()
  }

  fun revoke(request: EventBusCmdDataRequest): Query {
    val userId = request.userId ?: throw ApiStatus.MISSING_USER_ID

    return Jooq.postgres
      .update(SESSIONS)
      .set(
        mapOf(
          SESSIONS.REVOKED_AT to OffsetDateTime.now(),
        ),
      ).where(SESSIONS.USER_ID.eq(userId))
      .and(SESSIONS.REVOKED_AT.isNull)
      .and(SESSIONS.EXPIRES_AT.isNotNull.and(SESSIONS.EXPIRES_AT.ge(OffsetDateTime.now())))
      .returning()
  }

  private fun buildConditions(filters: Map<String, List<String>>): Array<Condition> =
    filters
      .mapNotNull { (field, values) ->
        when (field) {
          SESSIONS.REFRESH_TOKEN_HASH.name -> SESSIONS.REFRESH_TOKEN_HASH.`in`(values.map { it.toByteArray(Charsets.UTF_8) })
          SESSIONS.USER_ID.name -> SESSIONS.USER_ID.`in`(values.map { it.toUUID() })
          else -> null
        }
      }.toTypedArray()
}
