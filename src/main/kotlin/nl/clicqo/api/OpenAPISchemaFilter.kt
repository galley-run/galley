package nl.clicqo.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.openapi.contract.OpenAPIContract
import org.slf4j.LoggerFactory

object OpenAPISchemaFilter {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun filterBySchema(
    data: Any?,
    contract: OpenAPIContract,
    operationId: String,
    statusCode: Int,
    contentType: String,
  ): Any? {
    if (data == null) return null

    val allowedFields = getAllowedFields(contract, operationId, statusCode, contentType)

    if (allowedFields == null) {
      logger.warn("Could not get allowed fields for operationId=$operationId, statusCode=$statusCode - returning unfiltered data")
      return data
    }

    return when (data) {
      is JsonObject -> filterJsonObject(data, allowedFields)
      is JsonArray -> filterJsonArray(data, allowedFields)
      else -> data
    }
  }

  private fun filterJsonObject(
    obj: JsonObject,
    allowedFields: Set<String>,
  ): JsonObject {
    val filtered = JsonObject()

    if (!obj.containsKey("attributes")) {
      return JsonObject(obj.map.filterKeys { it in allowedFields })
    }

    // Always preserve id, type - these are JSON:API top-level fields
    obj.getString("id")?.let { filtered.put("id", it) }
    obj.getString("type")?.let { filtered.put("type", it) }

    // Filter the attributes object
    val attributes = obj.getJsonObject("attributes")
    if (attributes != null) {
      val filteredAttributes = JsonObject(attributes.map.filterKeys { it in allowedFields })
      val removedFields = attributes.fieldNames() - allowedFields
      filtered.put("attributes", filteredAttributes)
    }

    return filtered
  }

  private fun filterJsonArray(
    arr: JsonArray,
    allowedFields: Set<String>,
  ): JsonArray =
    JsonArray(
      arr.list.map {
        if (it is JsonObject) filterJsonObject(it, allowedFields) else it
      },
    )

  /**
   * Extracts property names from the 'attributes' object within a JSON:API schema.
   * Handles allOf composition to merge attributes from all sub-schemas.
   */
  private fun extractAttributesFields(schema: JsonObject?): Set<String> {
    if (schema == null) return emptySet()

    val allAttributeFields = mutableSetOf<String>()

    // Handle allOf composition
    val allOf = schema.getJsonArray("allOf")
    if (allOf != null) {
      for (i in 0 until allOf.size()) {
        val subSchema = allOf.getJsonObject(i)
        // Recursively extract attributes fields from each sub-schema
        allAttributeFields.addAll(extractAttributesFields(subSchema))
      }
    }

    // Look for the attributes property
    val properties = schema.getJsonObject("properties")
    if (properties != null) {
      val attributesSchema = properties.getJsonObject("attributes")
      if (attributesSchema != null) {
        val attributesProperties = attributesSchema.getJsonObject("properties")
        if (attributesProperties != null) {
          allAttributeFields.addAll(attributesProperties.fieldNames())
        }
      }
    }

    return allAttributeFields
  }

  private fun getAllowedFields(
    contract: OpenAPIContract,
    operationId: String,
    statusCode: Int,
    contentType: String,
  ): Set<String>? {
    try {
      val operation = contract.operation(operationId)
      if (operation == null) {
        logger.warn("Operation not found: $operationId")
        return null
      }

      val response = operation.getResponse(statusCode) ?: operation.defaultResponse
      if (response == null) {
        logger.warn("Response not found for operationId=$operationId, statusCode=$statusCode")
        return null
      }

      // Get the schema from the response content for the specific contentType
      val content = response.content
      if (content == null) {
        logger.warn("No content for response")
        return null
      }

      val mediaTypeObject = content[contentType]
      if (mediaTypeObject == null) {
        logger.warn("No media type found for contentType=$contentType, available types: ${content.keys}")
        return null
      }

      val schema = mediaTypeObject.schema
      if (schema == null) {
        logger.warn("No schema found")
        return null
      }

      // Navigate to data.properties to get the actual object schema
      val properties = schema.get<JsonObject>("properties")
      if (properties == null) {
        logger.warn("No properties in schema")
        return null
      }

      val dataSchema = properties.getJsonObject("data")
      if (dataSchema == null) {
        logger.warn("No 'data' property in schema")
        return null
      }

      // Handle both direct object schema and array items
      val objectSchema =
        when {
          dataSchema.getString("type") == "array" -> {
            dataSchema.getJsonObject("items")
          }
          else -> {
            dataSchema
          }
        }

      // Extract properties from the 'attributes' object specifically
      val attributesFields = extractAttributesFields(objectSchema)
      if (attributesFields.isEmpty()) {
        return dataSchema.getJsonObject("properties").map.keys
      }

      return attributesFields
    } catch (e: Exception) {
      logger.error("Error getting allowed fields for operationId=$operationId", e)
      return null
    }
  }
}
