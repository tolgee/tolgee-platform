package io.tolgee.mcp.tools

import io.modelcontextprotocol.server.McpServerFeatures
import io.tolgee.model.key.Key
import io.tolgee.service.key.KeyService
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.modelcontextprotocol.spec.McpSchema.JsonSchema
import io.modelcontextprotocol.spec.McpSchema.TextContent

fun textResult(text: String): CallToolResult {
  return CallToolResult
    .builder()
    .content(listOf(TextContent(text)))
    .isError(false)
    .build()
}

fun errorResult(message: String): CallToolResult {
  return CallToolResult
    .builder()
    .content(listOf(TextContent(message)))
    .isError(true)
    .build()
}

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

fun McpSyncServer.addTool(
  name: String,
  description: String,
  schema: JsonSchema,
  handler: (McpSchema.CallToolRequest) -> CallToolResult,
) {
  val tool =
    McpSchema.Tool
      .builder()
      .name(name)
      .description(description)
      .inputSchema(schema)
      .build()

  addTool(
    McpServerFeatures.SyncToolSpecification(
      tool,
      null,
    ) { exchange, request ->
      handler(request)
    },
  )
}

fun KeyService.resolveKeysByName(
  projectId: Long,
  names: List<String>,
  namespace: String?,
  branch: String?,
): Pair<List<Key>, List<String>> {
  val resolved =
    names.map { name ->
      name to find(projectId = projectId, name = name, namespace = namespace, branch = branch)
    }
  val found = resolved.mapNotNull { it.second }
  val notFound = resolved.filter { it.second == null }.map { it.first }
  return found to notFound
}

fun Map<String, Any?>.getLong(key: String): Long? = (this[key] as? Number)?.toLong()

fun Map<String, Any?>.getString(key: String): String? = this[key] as? String

fun Map<String, Any?>.getInt(key: String): Int? = (this[key] as? Number)?.toInt()

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getStringList(key: String): List<String>? = this[key] as? List<String>

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getStringMap(key: String): Map<String, String?>? = this[key] as? Map<String, String?>

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getList(key: String): List<Map<String, Any?>>? = this[key] as? List<Map<String, Any?>>

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getLongList(key: String): List<Long>? =
  (this[key] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }
