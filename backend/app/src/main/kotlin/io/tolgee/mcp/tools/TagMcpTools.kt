package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.TagsController
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.TagService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class TagMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val tagService: TagService,
  private val keyService: KeyService,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listTagsSpec = buildSpec(TagsController::getAll, "list_tags")
  private val tagKeysSpec = buildSpec(TagsController::tagKey, "tag_keys")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_tags",
      "List all tags used in a Tolgee project. Returns up to 1000 results per page.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("search", "Optional: search filter for tag names")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(listTagsSpec, projectId) {
        val search = request.arguments.getString("search")
        val pageNum = request.arguments.getInt("page") ?: 0
        val tags =
          tagService.getProjectTags(
            projectId = projectId,
            search = search,
            pageable = PageRequest.of(pageNum, 1000, Sort.by("name")),
          )
        val result =
          mapOf(
            "items" to
              tags.content.map { tag ->
                mapOf(
                  "id" to tag.id,
                  "name" to tag.name,
                )
              },
            "page" to tags.number,
            "totalPages" to tags.totalPages,
            "totalItems" to tags.totalElements,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "tag_keys",
      "Add tags to translation keys. Creates tags if they don't exist.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        stringArray("keyNames", "Names of the keys to tag", required = true)
        stringArray("tags", "Names of the tags to add", required = true)
        string("namespace", "Optional: namespace of the keys")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(tagKeysSpec, projectId) {
        val keyNames = request.arguments.requireStringList("keyNames")
        val tags = request.arguments.requireStringList("tags")
        val namespace = request.arguments.getString("namespace")
        val branch = request.arguments.getString("branch")

        val (keys, notFound) = keyService.resolveKeysByName(projectId, keyNames, namespace, branch)
        if (notFound.isNotEmpty()) {
          errorResult("Keys not found: ${notFound.joinToString(", ")}")
        } else {
          val keyIdToTags = keys.associate { key -> key.id to tags }
          tagService.tagKeysById(projectId, keyIdToTags)
          textResult(
            objectMapper.writeValueAsString(
              mapOf(
                "tagged" to true,
                "keyCount" to keyNames.size,
                "tags" to tags,
              ),
            ),
          )
        }
      }
    }
  }
}
