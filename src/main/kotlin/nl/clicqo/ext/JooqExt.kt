package nl.clicqo.ext

import io.vertx.core.json.JsonArray
import org.jooq.DeleteConditionStep
import org.jooq.DeleteUsingStep
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.SelectWhereStep
import org.jooq.TableField
import org.jooq.UpdateConditionStep
import org.jooq.UpdateSetMoreStep
import org.jooq.UpdateWhereStep
import java.util.UUID
import nl.clicqo.api.ApiPagination

fun <R : Record?> SelectWhereStep<R>.applyIdentifier(
  field: TableField<R, String?>,
  identifier: String,
): SelectConditionStep<R> {
  return this.where(field.eq(identifier))
}

fun <R : Record?> SelectWhereStep<R>.applyIdentifier(
  field: TableField<R, UUID?>,
  identifier: UUID,
): SelectConditionStep<R> {
  return this.where(field.eq(identifier))
}

fun <R : Record?> UpdateWhereStep<R>.applyIdentifier(
  field: TableField<R, UUID?>,
  identifier: UUID,
): UpdateConditionStep<R> {
  return this.where(field.eq(identifier))
}

fun <R : Record?> SelectWhereStep<R>.applyIdentifier(
  field: TableField<R, Int?>,
  identifier: Int,
): SelectConditionStep<R> {
  return this.where(field.eq(identifier))
}

fun <R : Record?> SelectWhereStep<R>.applyConditions(conditions: JsonArray?): SelectConditionStep<R> {
  return this.where().applyConditions(conditions)
}

fun <R : Record?> DeleteUsingStep<R>.applyConditions(conditions: JsonArray?): DeleteConditionStep<R> {
  return this.where().applyConditions(conditions)
}

fun <R : Record?> SelectConditionStep<R>.applyConditions(conditions: JsonArray?): SelectConditionStep<R> {
  conditions?.forEach {
    this.and(it.toString())
  }

  return this
}

fun <R : Record?> DeleteConditionStep<R>.applyConditions(conditions: JsonArray?): DeleteConditionStep<R> {
  conditions?.forEach {
    this.and(it.toString())
  }

  return this
}

fun <R : Record?> UpdateSetMoreStep<R>.applyConditions(conditions: JsonArray?): UpdateConditionStep<R> {
  val record = this.where()

  conditions?.forEach {
    record.and(it.toString())
  }

  return record
}

fun <R : Record?> UpdateConditionStep<R>.applyConditions(conditions: JsonArray?): UpdateConditionStep<R> {
  conditions?.forEach {
    this.and(it.toString())
  }

  return this
}

fun <R : Record?> SelectWhereStep<R>.applyPagination(pagination: ApiPagination?): SelectConditionStep<R> {
  return this.where().applyPagination(pagination)
}

fun <R : Record?> SelectConditionStep<R>.applyPagination(pagination: ApiPagination?): SelectConditionStep<R> {
  if (pagination == null) {
    return this
  }

  this.limit(pagination.sqlOffset, pagination.limit)

  return this
}
