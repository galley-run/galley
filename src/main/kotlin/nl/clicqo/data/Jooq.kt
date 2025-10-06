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

object Jooq {
  val postgres by lazy {
    DSL.using(
      SQLDialect.POSTGRES, Settings()
        .withBackslashEscaping(BackslashEscaping.OFF)
        .withRenderSchema(false)
        .withParamType(ParamType.NAMED)
        .withRenderNamedParamPrefix("$")
    )
  }

  fun prepareBindValues(values: List<Any?>) =
    values.map {
      if (it != null && it.javaClass.isEnum) {
        return@map (it.javaClass.enumConstants as Array<Enum<*>>).first { enum -> enum == it }.name
      }
      return@map it
    }
}

suspend fun Pool.executePreparedQuery(query: Query): RowSet<Row>? {
  val results = this.preparedQuery(query.sql)
    .execute(Tuple.wrap(Jooq.prepareBindValues(query.bindValues)))
    .coAwait()

  return results
}
