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
  private val tagKeySpec = buildSpec(TagsController::tagKey, "tag_key")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_tags",
      "List all tags used in a Tolgee project",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("search", "Optional: search filter for tag names")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(listTagsSpec, projectId) {
        val search = request.arguments.getString("search")
        val tags =
          tagService.getProjectTags(
            projectId = projectId,
            search = search,
            pageable = PageRequest.of(0, 1000, Sort.by("name")),
          )
        val result =
          tags.content.map { tag ->
            mapOf(
              "id" to tag.id,
              "name" to tag.name,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "tag_key",
      "Add a tag to a translation key. Creates the tag if it doesn't exist.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        number("keyId", "ID of the key to tag", required = true)
        string("tagName", "Name of the tag to add", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(tagKeySpec, projectId) {
        val keyId = request.arguments.getLong("keyId")!!
        val tagName = request.arguments.getString("tagName") ?: ""
        val tag = tagService.tagKey(projectId, keyId, tagName)
        val result =
          mapOf(
            "id" to tag.id,
            "name" to tag.name,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }
  }
}
