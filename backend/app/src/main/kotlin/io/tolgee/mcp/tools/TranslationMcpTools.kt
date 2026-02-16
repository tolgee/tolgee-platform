package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeyService
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Component

@Component
class TranslationMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val keyService: KeyService,
  private val translationService: TranslationService,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val getTranslationsSpec = buildSpec(TranslationsController::getAllTranslations, "get_translations")
  private val setTranslationsSpec = buildSpec(TranslationsController::setTranslations, "set_translation")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "get_translations",
      "Get translations for a specific key in a Tolgee project. Returns translations in all project languages, or only the specified languages if provided.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "The translation key name", required = true)
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
        stringArray(
          "languages",
          "Optional: language tags to filter by (e.g. ['en', 'de']). If omitted, returns all languages.",
        )
      },
    ) { request ->
      mcpRequestContext.executeAs(getTranslationsSpec, request.arguments.getProjectId()) {
        val key =
          keyService.find(
            projectId = projectHolder.project.id,
            name = request.arguments.requireString("keyName"),
            namespace = request.arguments.getString("namespace"),
            branch = request.arguments.getString("branch"),
          )

        if (key == null) {
          errorResult("Key not found")
        } else {
          val languageTags = request.arguments.getStringList("languages")
          val translations =
            if (languageTags.isNullOrEmpty()) {
              translationService.getAllByKeyId(key.id)
            } else {
              translationService.findForKeyByLanguages(key, languageTags)
            }
          val result =
            mapOf(
              "keyId" to key.id,
              "keyName" to key.name,
              "namespace" to key.namespace?.name,
              "translations" to
                translations.map { t ->
                  mapOf(
                    "languageTag" to t.language.tag,
                    "text" to t.text,
                    "state" to t.state.name,
                  )
                },
            )
          textResult(objectMapper.writeValueAsString(result))
        }
      }
    }

    server.addTool(
      "set_translation",
      "Set or update translations for a key in one or more languages. The key must already exist — use create_keys to create it first.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "The translation key name", required = true)
        string("namespace", "Optional: namespace of the key")
        stringMap(
          "translations",
          "Translations as {languageTag: text} map (e.g. {\"en\": \"Hello\", \"de\": \"Hallo\"})",
          required = true,
        )
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(setTranslationsSpec, request.arguments.getProjectId()) {
        val dto =
          SetTranslationsWithKeyDto(
            key = request.arguments.requireString("keyName"),
            namespace = request.arguments.getString("namespace"),
            translations = request.arguments.requireStringMap("translations"),
            branch = request.arguments.getString("branch"),
          )
        val key =
          keyService.find(
            projectId = projectHolder.project.id,
            name = dto.key,
            namespace = dto.namespace,
            branch = dto.branch,
          )

        if (key != null) {
          val translations = translationService.setForKey(key, dto.translations)
          val result =
            mapOf(
              "keyId" to key.id,
              "keyName" to key.name,
              "translations" to
                translations.map { (tag, t) ->
                  mapOf("languageTag" to tag, "text" to t.text, "state" to t.state.name)
                },
            )
          textResult(objectMapper.writeValueAsString(result))
        } else {
          errorResult("Key '${dto.key}' not found. Use create_keys to create it first.")
        }
      }
    }
  }
}
