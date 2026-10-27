package io.tolgee.mcp.tools

import io.modelcontextprotocol.server.McpStatelessSyncServer
import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.service.translation.TranslationSuggestionService
import io.tolgee.util.executeInNewTransaction
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnClass(name = ["io.tolgee.ee.service.TranslationSuggestionServiceEeImpl"])
class SuggestionMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val translationSuggestionService: TranslationSuggestionService,
  private val translationService: TranslationService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : McpToolsProvider {
  companion object {
    private val suggestionControllerClass =
      Class.forName("io.tolgee.ee.api.v2.controllers.SuggestionController")
    private val long = Long::class.javaPrimitiveType!!
  }

  private val listKeysSpec = buildSpec(TranslationsController::getTranslations, "list_keys_with_suggestions")
  private val listSpec =
    buildSpec(
      suggestionControllerClass,
      "getSuggestions",
      "list_suggestions",
      Pageable::class.java,
      SuggestionFilters::class.java,
      long,
      long,
    )
  private val acceptSpec =
    buildSpec(
      suggestionControllerClass,
      "acceptSuggestion",
      "accept_suggestion",
      long,
      long,
      long,
      Boolean::class.javaPrimitiveType!!,
    )
  private val declineSpec =
    buildSpec(suggestionControllerClass, "declineSuggestion", "decline_suggestion", long, long, long)

  override fun register(server: McpStatelessSyncServer) {
    server.addTool(
      "list_keys_with_suggestions",
      "List keys that have pending (active) translation suggestions in a language. " +
        "Returns up to 100 keys per page; use list_suggestions on a key to see the suggestions themselves.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        string("branch", "Optional: branch name")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      mcpRequestContext.executeAs(listKeysSpec, request.arguments.getProjectId()) {
        val tag = request.arguments.requireString("languageTag")
        val projectId = projectHolder.project.id
        val languages =
          languageService.getLanguagesForTranslationsView(
            setOf(tag),
            projectId,
            authenticationFacade.authenticatedUser.id,
          )
        if (languages.isEmpty()) return@executeAs errorResult("Language not found or not viewable")
        val params =
          GetTranslationsParams().apply {
            this.languages = setOf(tag)
            filterHasSuggestionsInLang = listOf(tag)
            branch = request.arguments.getString("branch")
          }
        val pageNum = request.arguments.getInt("page") ?: 0
        val page =
          translationService.getViewData(
            projectId,
            PageRequest.of(pageNum, 100, Sort.by("keyId")),
            params,
            languages,
          )
        val result =
          pagedResponse(page) { key ->
            mapOf(
              "keyId" to key.keyId,
              "keyName" to key.keyName,
              "keyNamespace" to key.keyNamespace,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "list_suggestions",
      "List translation suggestions for a key in one language. Returns up to 100 results per page.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "The translation key name", required = true)
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
        stringArray("states", "Optional: only suggestions in these states (ACTIVE, ACCEPTED, DECLINED). Default: all.")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      mcpRequestContext.executeAs(listSpec, request.arguments.getProjectId()) {
        withKeyAndLanguage(request.arguments) { projectId, key, language ->
          securityService.checkLanguageViewSuggestionsPermission(projectId, listOf(language.id))
          val states =
            request.arguments.getStringList("states")?.takeIf { it.isNotEmpty() }?.map { name ->
              TranslationSuggestionState.entries.firstOrNull { it.name == name }
                ?: return@withKeyAndLanguage errorResult(
                  "Unknown state '$name', allowed: ${TranslationSuggestionState.entries.joinToString()}",
                )
            }
          val filters = SuggestionFilters().apply { filterState = states }
          val pageNum = request.arguments.getInt("page") ?: 0
          val page =
            translationSuggestionService.getSuggestionsPaged(
              PageRequest.of(pageNum, 100, Sort.by("id")),
              projectId,
              language.id,
              key.id,
              filters,
            )
          textResult(objectMapper.writeValueAsString(pagedResponse(page) { suggestionResult(it) }))
        }
      }
    }

    server.addTool(
      "accept_suggestion",
      "Accept a translation suggestion: its text becomes the translation of the key in that language. " +
        "With declineOther, all other active suggestions for the same key and language are declined.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "The translation key name", required = true)
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        number("suggestionId", "ID of the suggestion (from list_suggestions)", required = true)
        boolean(
          "declineOther",
          "Optional: decline the other active suggestions of this key and language (default false)",
        )
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(acceptSpec, request.arguments.getProjectId()) {
        withKeyAndLanguage(request.arguments) { projectId, key, language ->
          securityService.checkLanguageTranslatePermission(projectId, listOf(language.id))
          securityService.checkLanguageStateChangePermission(projectId, listOf(language.id))
          executeInNewTransaction(transactionManager) {
            val (suggestion, declined) =
              translationSuggestionService.acceptSuggestion(
                projectId,
                language.id,
                key.id,
                request.arguments.requireLong("suggestionId"),
                request.arguments.getBoolean("declineOther") ?: false,
              ) ?: return@executeInNewTransaction errorResult("Suggestions are not available on this instance")
            textResult(
              objectMapper.writeValueAsString(
                mapOf("accepted" to suggestionResult(suggestion), "declined" to declined),
              ),
            )
          }
        }
      }
    }

    server.addTool(
      "decline_suggestion",
      "Decline a translation suggestion.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "The translation key name", required = true)
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        number("suggestionId", "ID of the suggestion (from list_suggestions)", required = true)
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(declineSpec, request.arguments.getProjectId()) {
        withKeyAndLanguage(request.arguments) { projectId, key, language ->
          securityService.checkLanguageStateChangePermission(projectId, listOf(language.id))
          executeInNewTransaction(transactionManager) {
            val suggestion =
              translationSuggestionService.declineSuggestion(
                projectId,
                language.id,
                key.id,
                request.arguments.requireLong("suggestionId"),
              ) ?: return@executeInNewTransaction errorResult("Suggestions are not available on this instance")
            textResult(objectMapper.writeValueAsString(suggestionResult(suggestion)))
          }
        }
      }
    }
  }

  private fun withKeyAndLanguage(
    arguments: Map<String, Any?>,
    block: (Long, Key, LanguageDto) -> CallToolResult,
  ): CallToolResult {
    val projectId = projectHolder.project.id
    val key =
      keyService.find(
        projectId = projectId,
        name = arguments.requireString("keyName"),
        namespace = arguments.getString("namespace"),
        branch = arguments.getString("branch"),
      ) ?: return errorResult("Key not found")
    val language =
      languageService.findByTag(arguments.requireString("languageTag"), projectId)
        ?: return errorResult("Language not found")
    return block(projectId, key, language)
  }

  private fun suggestionResult(suggestion: TranslationSuggestion) =
    mapOf(
      "id" to suggestion.id,
      "translation" to suggestion.translation,
      "state" to suggestion.state.name,
      "isPlural" to suggestion.isPlural,
      "author" to suggestion.author?.let { mapOf("id" to it.id, "name" to it.name) },
      "createdAt" to suggestion.createdAt,
    )
}
