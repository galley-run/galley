package nl.clicqo.data

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.jooq.Query
import org.jooq.SQLDialect
import org.jooq.conf.BackslashEscaping
import org.jooq.conf.ParamType
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.postgres.extensions.types.Inet
import io.vertx.pgclient.data.Inet as PgInet

object Jooq {
  val postgres by lazy {
    DSL.using(
      SQLDialect.POSTGRES,
      Settings()
        .withBackslashEscaping(BackslashEscaping.OFF)
        .withRenderSchema(false)
        .withParamType(ParamType.NAMED)
        .withRenderNamedParamPrefix("$"),
    )
  }
}

@Suppress("SqlSourceToSinkFlow")
suspend fun Pool.execute(query: Query): RowSet<Row>? {
  val rawParams: List<Any?> = query.bindValues

  val mappedParams: List<Any?> =
    rawParams.map { v ->
      when (v) {
        is Inet ->
          PgInet()
            .setAddress(v.address()) // java.net.InetAddress
            .setNetmask(v.prefix()) // kan null zijn
        else -> v
      }
    }

  val tuple: Tuple = Tuple.from(mappedParams)

  val results =
    this
      .preparedQuery(query.sql)
      .execute(tuple)
      .coAwait()

  return results
}
