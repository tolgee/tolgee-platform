package io.tolgee.mcp.tools

import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.modelcontextprotocol.spec.McpSchema.JsonSchema
import io.modelcontextprotocol.spec.McpSchema.TextContent
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.security.ProjectNotSelectedException

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

fun Map<String, Any?>.getProjectId(): Long = getLong("projectId") ?: throw ProjectNotSelectedException()

private fun missingParam(key: String): Nothing =
  throw BadRequestException(Message.REQUEST_VALIDATION_ERROR, listOf(key))

fun Map<String, Any?>.getLong(key: String): Long? = (this[key] as? Number)?.toLong()

fun Map<String, Any?>.requireLong(key: String): Long = getLong(key) ?: missingParam(key)

fun Map<String, Any?>.getString(key: String): String? = this[key] as? String

fun Map<String, Any?>.requireString(key: String): String = getString(key) ?: missingParam(key)

fun Map<String, Any?>.getInt(key: String): Int? = (this[key] as? Number)?.toInt()

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getStringList(key: String): List<String>? = this[key] as? List<String>

fun Map<String, Any?>.requireStringList(key: String): List<String> = getStringList(key) ?: missingParam(key)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getStringMap(key: String): Map<String, String?>? = this[key] as? Map<String, String?>

fun Map<String, Any?>.requireStringMap(key: String): Map<String, String?> = getStringMap(key) ?: missingParam(key)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getList(key: String): List<Map<String, Any?>>? = this[key] as? List<Map<String, Any?>>

fun Map<String, Any?>.requireList(key: String): List<Map<String, Any?>> = getList(key) ?: missingParam(key)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.getLongList(key: String): List<Long>? =
  (this[key] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }
