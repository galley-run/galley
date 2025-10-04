package nl.clicqo.ext

import io.vertx.core.json.JsonArray
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import nl.clicqo.data.DataModel

fun RowSet<Row>?.mapToCount(): Int {
  return this!!.firstOrNull()?.run {
    return@run this.getInteger("count")
  } ?: 0
}

fun RowSet<Row>?.mapToDataModel(fn: (Row) -> DataModel): Collection<DataModel> =
  this!!.map {
    fn(it)
  }

fun Row.mapToDataModel(fn: (Row) -> DataModel): DataModel = fn(this)

fun Collection<DataModel>.toJsonArray(): JsonArray {
  val jsonArray = JsonArray()
  this.forEach {
    jsonArray.add(it.toJsonObject())
  }
  return jsonArray
}

