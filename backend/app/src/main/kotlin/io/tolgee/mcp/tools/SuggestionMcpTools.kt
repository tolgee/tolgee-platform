package io.tolgee.mcp.tools

import io.modelcontextprotocol.spec.McpSchema.CallToolResult
import io.tolgee.api.v2.controllers.LanguagesFacade
import io.tolgee.api.v2.controllers.keys.KeyFacade
import io.tolgee.api.v2.controllers.suggestion.SuggestionsFacade
import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
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
  private val suggestionsFacade: SuggestionsFacade,
  private val keyFacade: KeyFacade,
  private val languagesFacade: LanguagesFacade,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : McpToolsProvider {
  companion object {
    private val suggestionControllerClass =
      Class.forName("io.tolgee.ee.api.v2.controllers.SuggestionController")
    private val long = Long::class.javaPrimitiveType!!
    private const val PAGE_SIZE = 100
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

  override fun register(registrar: McpToolRegistrar) {
    registrar.addTool(
      "list_keys_with_suggestions",
      "List keys with suggestions",
      McpToolEffect.READ_ONLY,
      "List keys that have pending (active) translation suggestions in a language. " +
        "Returns up to $PAGE_SIZE keys per page; use list_suggestions on a key to see the suggestions themselves.",
      toolSchema {
        projectId()
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        string("branch", "Optional: branch name")
        number("page", "Optional: page number (0-based, default 0)")
      },
      spec = listKeysSpec,
    ) { request ->
      mcpRequestContext.executeAs(listKeysSpec, request.arguments.getProjectId()) {
        val tag = request.arguments.requireString("languageTag")
        val pageNum = request.arguments.getInt("page") ?: 0
        val page =
          suggestionsFacade.getKeysWithSuggestions(
            tag,
            request.arguments.getString("branch"),
            PageRequest.of(pageNum, PAGE_SIZE, Sort.by("keyId")),
          ) ?: return@executeAs errorResult("Language not found or not viewable")
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

    registrar.addTool(
      "list_suggestions",
      "List suggestions",
      McpToolEffect.READ_ONLY,
      "List translation suggestions for a key in one language. Returns up to $PAGE_SIZE results per page.",
      toolSchema {
        projectId()
        string("keyName", "The translation key name", required = true)
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
        stringArray("states", "Optional: only suggestions in these states (ACTIVE, ACCEPTED, DECLINED). Default: all.")
        number("page", "Optional: page number (0-based, default 0)")
      },
      spec = listSpec,
    ) { request ->
      mcpRequestContext.executeAs(listSpec, request.arguments.getProjectId()) {
        withKeyAndLanguage(request.arguments) { key, language ->
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
            suggestionsFacade.getSuggestions(
              language.id,
              key.id,
              filters,
              PageRequest.of(pageNum, PAGE_SIZE, Sort.by("id")),
            )
          textResult(objectMapper.writeValueAsString(pagedResponse(page) { suggestionResult(it) }))
        }
      }
    }

    registrar.addTool(
      "accept_suggestion",
      "Accept suggestion",
      McpToolEffect.DESTRUCTIVE,
      "Accept a translation suggestion: its text becomes the translation of the key in that language. " +
        "With declineOther, all other active suggestions for the same key and language are declined.",
      toolSchema {
        projectId()
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
      spec = acceptSpec,
    ) { request ->
      mcpRequestContext.executeAs(acceptSpec, request.arguments.getProjectId()) {
        withKeyAndLanguage(request.arguments) { key, language ->
          executeInNewTransaction(transactionManager) {
            val (suggestion, declined) =
              suggestionsFacade.acceptSuggestion(
                language.id,
                key.id,
                request.arguments.requireLong("suggestionId"),
                request.arguments.getBoolean("declineOther") ?: false,
              )
            textResult(
              objectMapper.writeValueAsString(
                mapOf("accepted" to suggestionResult(suggestion), "declined" to declined),
              ),
            )
          }
        }
      }
    }

    registrar.addTool(
      "decline_suggestion",
      "Decline suggestion",
      McpToolEffect.DESTRUCTIVE,
      "Decline a translation suggestion.",
      toolSchema {
        projectId()
        string("keyName", "The translation key name", required = true)
        string("languageTag", "Language tag (e.g. 'de')", required = true)
        number("suggestionId", "ID of the suggestion (from list_suggestions)", required = true)
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
      },
      spec = declineSpec,
    ) { request ->
      mcpRequestContext.executeAs(declineSpec, request.arguments.getProjectId()) {
        withKeyAndLanguage(request.arguments) { key, language ->
          executeInNewTransaction(transactionManager) {
            val suggestion =
              suggestionsFacade.declineSuggestion(
                language.id,
                key.id,
                request.arguments.requireLong("suggestionId"),
              )
            textResult(objectMapper.writeValueAsString(suggestionResult(suggestion)))
          }
        }
      }
    }
  }

  private fun withKeyAndLanguage(
    arguments: Map<String, Any?>,
    block: (Key, LanguageDto) -> CallToolResult,
  ): CallToolResult {
    val key =
      keyFacade.find(
        name = arguments.requireString("keyName"),
        namespace = arguments.getString("namespace"),
        branch = arguments.getString("branch"),
      ) ?: return errorResult("Key not found")
    val language =
      languagesFacade.findByTag(arguments.requireString("languageTag"))
        ?: return errorResult("Language not found")
    return block(key, language)
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
