package nl.clicqo.ext

import io.vertx.core.json.JsonObject
import nl.clicqo.api.ApiStatus
import nl.clicqo.api.Pagination
import org.jooq.Condition
import org.jooq.DeleteConditionStep
import org.jooq.DeleteWhereStep
import org.jooq.Record
import org.jooq.SelectConditionStep
import org.jooq.SelectWhereStep
import org.jooq.TableField
import org.jooq.UpdateConditionStep
import org.jooq.UpdateWhereStep
import org.jooq.impl.TableRecordImpl
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

fun <R : Record?> SelectWhereStep<R>.applyPagination(pagination: Pagination?): SelectConditionStep<R> =
  this.where().applyPagination(pagination)

fun <R : Record?> SelectConditionStep<R>.applyPagination(pagination: Pagination?): SelectConditionStep<R> {
  if (pagination == null) {
    return this
  }

  this.limit(pagination.offset, pagination.limit)

  return this
}

fun <R : Record?> SelectWhereStep<R>.whereNotDeleted(deletedAtField: TableField<R, OffsetDateTime?>): SelectConditionStep<R> =
  this.where(deletedAtField.isNull.or(deletedAtField.gt(OffsetDateTime.now())))

fun <R : Record?> UpdateWhereStep<R>.whereNotDeleted(deletedAtField: TableField<R, OffsetDateTime?>): UpdateConditionStep<R> =
  this.where(deletedAtField.isNull.or(deletedAtField.gt(OffsetDateTime.now())))

fun <R : Record?> SelectConditionStep<R>.andNotDeleted(deletedAtField: TableField<R, OffsetDateTime?>): SelectConditionStep<R> =
  this.and(deletedAtField.isNull.or(deletedAtField.gt(OffsetDateTime.now())))

fun <R : Record?> UpdateConditionStep<R>.andNotDeleted(deletedAtField: TableField<R, OffsetDateTime?>): UpdateConditionStep<R> =
  this.and(deletedAtField.isNull.or(deletedAtField.gt(OffsetDateTime.now())))

fun <R : Record?> SelectWhereStep<R>.whereActivated(activatedAtField: TableField<R, OffsetDateTime?>): SelectConditionStep<R> =
  this.where(activatedAtField.isNotNull.and(activatedAtField.le(OffsetDateTime.now())))

fun <R : Record?> UpdateWhereStep<R>.whereActivated(activatedAtField: TableField<R, OffsetDateTime?>): UpdateConditionStep<R> =
  this.where(activatedAtField.isNotNull.and(activatedAtField.le(OffsetDateTime.now())))

fun <R : Record?> SelectConditionStep<R>.andActivated(activatedAtField: TableField<R, OffsetDateTime?>): SelectConditionStep<R> =
  this.and(activatedAtField.isNotNull.and(activatedAtField.le(OffsetDateTime.now())))

fun <R : Record?> UpdateConditionStep<R>.andActivated(activatedAtField: TableField<R, OffsetDateTime?>): UpdateConditionStep<R> =
  this.and(activatedAtField.isNotNull.and(activatedAtField.le(OffsetDateTime.now())))

// Overload for type-safe JOOQ Condition objects
private fun <R : Record?> checkRequiredConditions(
  requiredConditions: List<TableField<R, *>>?,
  vararg conditions: Condition,
) {
  if (requiredConditions == null) return

  val missingFields =
    requiredConditions.filter { required ->
      conditions.none { condition ->
        condition.toString().contains(required.name)
      }
    }

  if (missingFields.isNotEmpty()) {
    throw ApiStatus.JOOQ_MISSING_REQUIRED_FIELDS(missingFields.joinToString { it.name })
  }
}

fun <R : Record?> SelectWhereStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  vararg conditions: Condition,
): SelectConditionStep<R> {
  checkRequiredConditions(requiredConditions, *conditions)
  if (conditions.isEmpty()) {
    return this.where()
  }

  return this.where(*conditions)
}

fun <R : Record?> SelectConditionStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  vararg conditions: Condition,
): SelectConditionStep<R> {
  checkRequiredConditions(requiredConditions, *conditions)
  conditions.forEach {
    this.and(it)
  }
  return this
}

fun <R : Record?> UpdateWhereStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  vararg conditions: Condition,
): UpdateConditionStep<R> {
  checkRequiredConditions(requiredConditions, *conditions)
  if (conditions.isEmpty()) {
    return this.where()
  }

  return this.where(*conditions)
}

fun <R : Record?> UpdateConditionStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  vararg conditions: Condition,
): UpdateConditionStep<R> {
  checkRequiredConditions(requiredConditions, *conditions)
  conditions.forEach {
    this.and(it)
  }
  return this
}

fun <R : Record?> DeleteWhereStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  vararg conditions: Condition,
): DeleteConditionStep<R> {
  checkRequiredConditions(requiredConditions, *conditions)
  if (conditions.isEmpty()) {
    return this.where()
  }

  return this.where(*conditions)
}

fun <R : Record?> DeleteConditionStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  vararg conditions: Condition,
): DeleteConditionStep<R> {
  checkRequiredConditions(requiredConditions, *conditions)
  conditions.forEach {
    this.and(it)
  }
  return this
}

// Overload for type-safe JOOQ Condition objects
fun <R : Record?> SelectWhereStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  condition: Condition,
): SelectConditionStep<R> = this.applyConditions(requiredConditions, *listOf(condition).toTypedArray())

fun <R : Record?> SelectConditionStep<R>.applyConditions(
  requiredConditions: List<TableField<R, *>>? = null,
  condition: Condition,
): SelectConditionStep<R> = this.applyConditions(requiredConditions, *listOf(condition).toTypedArray())

fun <R : Record?> SelectWhereStep<R>.applyConditions(vararg conditions: Condition): SelectConditionStep<R> =
  this.applyConditions(null, *conditions)

fun <R : Record?> SelectConditionStep<R>.applyConditions(vararg conditions: Condition): SelectConditionStep<R> =
  this.applyConditions(null, *conditions)

fun <R : Record?> UpdateWhereStep<R>.applyConditions(vararg conditions: Condition): UpdateConditionStep<R> =
  this.applyConditions(null, *conditions)

fun <R : Record?> UpdateConditionStep<R>.applyConditions(vararg conditions: Condition): UpdateConditionStep<R> =
  this.applyConditions(null, *conditions)

fun <R : Record?> DeleteWhereStep<R>.applyConditions(vararg conditions: Condition): DeleteConditionStep<R> =
  this.applyConditions(null, *conditions)

fun <R : Record?> DeleteConditionStep<R>.applyConditions(vararg conditions: Condition): DeleteConditionStep<R> =
  this.applyConditions(null, *conditions)

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

/**
 * Converts a JsonObject to a jOOQ Record by automatically mapping JSON keys to record fields using reflection.
 * Only sets fields that exist in both the JsonObject and the Record type.
 *
 * Supports type conversion for:
 * - UUID (from String)
 * - OffsetDateTime (from String/Instant)
 * - Enums (from String)
 *
 * @param R the jOOQ Record type to create
 * @param table the jOOQ Table instance to create a new record from
 * @return a new Record instance with fields populated from the JsonObject
 *
 * @deprecated Create a Factory.toRecord() instead
 */
fun <R : TableRecordImpl<R>> JsonObject.toRecord(table: org.jooq.Table<R>): R {
  val record = table.newRecord()

  this.fieldNames().forEach { jsonKey ->
    val jsonValue = this.getValue(jsonKey)

    try {
      // Find matching field in record (jOOQ uses setter methods)
      val setterName = "set${jsonKey.replaceFirstChar { it.uppercase() }}"
      val setter = record::class.java.methods.find { it.name == setterName && it.parameterCount == 1 }

      if (setter != null) {
        val paramType = setter.parameterTypes[0]
        val convertedValue = if (jsonValue == null) null else convertValue(jsonValue, paramType)
        setter.invoke(record, convertedValue)
      }
    } catch (e: Exception) {
      // Ignore fields that can't be set
    }
  }

  return record
}

/**
 * Converts a value from JsonObject to the target type required by the jOOQ Record field.
 */
fun convertValue(
  value: Any,
  targetType: Class<*>,
): Any? =
  when {
    // Direct assignment if types match
    targetType.isInstance(value) -> {
      value
    }

    // UUID conversion
    targetType == UUID::class.java && value is String -> {
      UUID.fromString(value)
    }

    // OffsetDateTime conversion
    targetType == OffsetDateTime::class.java -> {
      when (value) {
        is String -> {
          try {
            Instant.parse(value).atOffset(ZoneOffset.UTC)
          } catch (e: Exception) {
            OffsetDateTime.parse(value)
          }
        }

        is Instant -> {
          value.atOffset(ZoneOffset.UTC)
        }

        else -> {
          null
        }
      }
    }

    // Enum conversion
    targetType.isEnum -> {
      @Suppress("UNCHECKED_CAST")
      val enumClass = targetType as Class<out Enum<*>>
      enumClass.enumConstants.find { it.name.equals(value.toString(), ignoreCase = true) }
    }

    // Number conversions
    targetType == Integer::class.java || targetType == Int::class.java -> {
      when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
      }
    }

    targetType == java.lang.Long::class.java || targetType == Long::class.java -> {
      when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
      }
    }

    targetType == java.lang.Double::class.java || targetType == Double::class.java -> {
      when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
      }
    }

    targetType == java.lang.Boolean::class.java || targetType == Boolean::class.java -> {
      when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull()
        else -> null
      }
    }

    // Default: try direct assignment
    else -> {
      value
    }
  }
