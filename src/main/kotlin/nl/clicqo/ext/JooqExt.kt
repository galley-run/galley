package nl.clicqo.ext

import nl.clicqo.api.ApiPagination
import org.jooq.Condition
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.SelectWhereStep
import org.jooq.TableField
import org.jooq.UpdateConditionStep
import org.jooq.UpdateWhereStep
import java.util.UUID

fun <R : Record?> SelectWhereStep<R>.applyPagination(pagination: ApiPagination?): SelectConditionStep<R> =
  this.where().applyPagination(pagination)

fun <R : Record?> SelectConditionStep<R>.applyPagination(pagination: ApiPagination?): SelectConditionStep<R> {
  if (pagination == null) {
    return this
  }

  this.limit(pagination.sqlOffset, pagination.limit)

  return this
}

// Overload for type-safe JOOQ Condition objects
fun <R : Record?> SelectWhereStep<R>.applyConditions(vararg conditions: Condition): SelectConditionStep<R> {
  if (conditions.isEmpty()) {
    return this.where()
  }
  return this.where(*conditions)
}

fun <R : Record?> SelectConditionStep<R>.applyConditions(vararg conditions: Condition): SelectConditionStep<R> {
  conditions.forEach {
    this.and(it)
  }
  return this
}

// Overload for type-safe JOOQ Condition objects
fun <R : Record?> SelectWhereStep<R>.applyConditions(condition: Condition): SelectConditionStep<R> = this.where(condition)

fun <R : Record?> SelectConditionStep<R>.applyConditions(condition: Condition): SelectConditionStep<R> = this.and(condition)

// Apply sorting with type-safe JOOQ SortField objects
fun <R : Record?> SelectConditionStep<R>.applySorting(sortFields: List<org.jooq.SortField<*>>): SelectConditionStep<R> {
  if (sortFields.isNotEmpty()) {
    this.orderBy(sortFields)
  }
  return this
}

// Apply pagination with offset and limit
fun <R : Record?> SelectConditionStep<R>.applyPagination(
  offset: Int,
  limit: Int,
): SelectConditionStep<R> {
  this.limit(limit).offset(offset)
  return this
}
