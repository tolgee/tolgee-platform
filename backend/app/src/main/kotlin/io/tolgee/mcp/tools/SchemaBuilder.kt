package io.tolgee.mcp.tools

import io.modelcontextprotocol.spec.McpSchema.JsonSchema

class SchemaBuilder {
  val properties = mutableMapOf<String, Map<String, Any?>>()
  val requiredFields = mutableListOf<String>()

  fun string(
    name: String,
    description: String,
    required: Boolean = false,
  ) {
    properties[name] = mapOf("type" to "string", "description" to description)
    if (required) requiredFields += name
  }

  fun number(
    name: String,
    description: String,
    required: Boolean = false,
  ) {
    properties[name] = mapOf("type" to "number", "description" to description)
    if (required) requiredFields += name
  }

  fun boolean(
    name: String,
    description: String,
    required: Boolean = false,
  ) {
    properties[name] = mapOf("type" to "boolean", "description" to description)
    if (required) requiredFields += name
  }

  fun stringMap(
    name: String,
    description: String,
    required: Boolean = false,
  ) {
    properties[name] =
      mapOf(
        "type" to "object",
        "description" to description,
        "additionalProperties" to mapOf("type" to "string"),
      )
    if (required) requiredFields += name
  }

  fun stringArray(
    name: String,
    description: String,
    required: Boolean = false,
  ) {
    properties[name] =
      mapOf(
        "type" to "array",
        "description" to description,
        "items" to mapOf("type" to "string"),
      )
    if (required) requiredFields += name
  }

  fun numberArray(
    name: String,
    description: String,
    required: Boolean = false,
  ) {
    properties[name] =
      mapOf(
        "type" to "array",
        "description" to description,
        "items" to mapOf("type" to "number"),
      )
    if (required) requiredFields += name
  }

  fun objectArray(
    name: String,
    description: String,
    required: Boolean = false,
    items: SchemaBuilder.() -> Unit,
  ) {
    val nested = SchemaBuilder().apply(items)
    val itemSchema =
      buildMap<String, Any?> {
        put("type", "object")
        put("properties", nested.properties)
        if (nested.requiredFields.isNotEmpty()) {
          put("required", nested.requiredFields)
        }
      }
    properties[name] =
      mapOf(
        "type" to "array",
        "description" to description,
        "items" to itemSchema,
      )
    if (required) requiredFields += name
  }

  @Suppress("UNCHECKED_CAST")
  fun build(): JsonSchema =
    JsonSchema(
      "object",
      properties as Map<String, Any?>,
      requiredFields,
      null,
      null,
      null,
    )
}

fun toolSchema(block: SchemaBuilder.() -> Unit): JsonSchema = SchemaBuilder().apply(block).build()
