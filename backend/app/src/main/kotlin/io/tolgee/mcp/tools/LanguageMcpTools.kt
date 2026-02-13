package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.V2LanguagesController
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.service.language.LanguageService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class LanguageMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val languageService: LanguageService,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listLanguagesSpec = buildSpec(V2LanguagesController::getAll, "list_languages")
  private val createLanguageSpec = buildSpec(V2LanguagesController::createLanguage, "create_language")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_languages",
      "List all languages configured for a Tolgee project. Returns up to 1000 results per page.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(listLanguagesSpec, projectId) {
        val pageNum = request.arguments.getInt("page") ?: 0
        val languages =
          languageService.getPaged(
            projectId,
            PageRequest.of(pageNum, 1000, Sort.by("tag")),
            null,
          )
        val result =
          mapOf(
            "items" to
              languages.content.map { lang ->
                mapOf(
                  "id" to lang.id,
                  "name" to lang.name,
                  "tag" to lang.tag,
                  "originalName" to lang.originalName,
                  "flagEmoji" to lang.flagEmoji,
                  "base" to lang.base,
                )
              },
            "page" to languages.number,
            "totalPages" to languages.totalPages,
            "totalItems" to languages.totalElements,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "create_language",
      "Add a new language to a Tolgee project",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("name", "Language display name (e.g. 'German')", required = true)
        string("tag", "Language tag / IETF BCP 47 code (e.g. 'de')", required = true)
        string("originalName", "Native name of the language (e.g. 'Deutsch')")
        string("flagEmoji", "Flag emoji for this language")
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(createLanguageSpec, projectId) {
        val dto =
          LanguageRequest(
            name = request.arguments.requireString("name"),
            tag = request.arguments.requireString("tag"),
            originalName = request.arguments.getString("originalName"),
            flagEmoji = request.arguments.getString("flagEmoji"),
          )
        val language = languageService.createLanguage(dto, projectHolder.projectEntity)
        val result =
          mapOf(
            "id" to language.id,
            "name" to language.name,
            "tag" to language.tag,
            "originalName" to language.originalName,
            "flagEmoji" to language.flagEmoji,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }
  }
}
