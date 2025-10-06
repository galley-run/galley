package nl.clicqo.ext

import java.util.UUID
import nl.clicqo.api.ApiPagination
import org.jooq.Condition
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.SelectWhereStep
import org.jooq.TableField
import org.jooq.UpdateConditionStep
import org.jooq.UpdateWhereStep

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

// Overload for type-safe JOOQ Condition objects
fun <R : Record?> SelectWhereStep<R>.applyConditions(conditions: List<Condition>): SelectConditionStep<R> {
  if (conditions.isEmpty()) {
    return this.where()
  }
  return this.where(conditions)
}

fun <R : Record?> SelectConditionStep<R>.applyConditions(conditions: List<Condition>): SelectConditionStep<R> {
  conditions.forEach {
    this.and(it)
  }
  return this
}

// Apply sorting with type-safe JOOQ SortField objects
fun <R : Record?> SelectConditionStep<R>.applySorting(sortFields: List<org.jooq.SortField<*>>): SelectConditionStep<R> {
  if (sortFields.isNotEmpty()) {
    this.orderBy(sortFields)
  }
  return this
}

// Apply pagination with offset and limit
fun <R : Record?> SelectConditionStep<R>.applyPagination(offset: Int, limit: Int): SelectConditionStep<R> {
  this.limit(limit).offset(offset)
  return this
}
