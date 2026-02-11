package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.keys.KeyController
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.translation.ImportKeysDto
import io.tolgee.dtos.request.translation.ImportKeysItemDto
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeyService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class KeyMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listKeysSpec = buildSpec(KeyController::getAll, "list_keys")
  private val searchKeysSpec = buildSpec(KeyController::searchForKey, "search_keys")
  private val createKeySpec = buildSpec(KeyController::create, "create_key")
  private val importKeysSpec =
    buildSpec(
      KeyController::class.java,
      "importKeys",
      "batch_create_keys",
      ImportKeysDto::class.java,
      String::class.java,
    )
  private val editKeySpec = buildSpec(KeyController::edit, "update_key")
  private val getKeySpec = buildSpec(KeyController::get, "get_key")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_keys",
      "List translation keys in a Tolgee project. Returns up to 100 keys sorted by ID. Use search_keys to find specific keys by name or translation text.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("branch", "Optional: branch name (for branching projects)")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(listKeysSpec, projectId) {
        val page =
          keyService.getPaged(
            projectId,
            request.arguments.getString("branch"),
            PageRequest.of(0, 100),
          )
        val keys =
          page.content.map { key ->
            mapOf(
              "keyId" to key.id,
              "keyName" to key.name,
              "keyNamespace" to key.namespace,
            )
          }
        val result =
          mapOf(
            "keys" to keys,
            "totalKeys" to page.totalElements,
            "hasMore" to (page.totalElements > 100),
            "hint" to
              if (page.totalElements > 100) {
                "Showing first 100 of ${page.totalElements} keys. Use search_keys to find specific keys."
              } else {
                null
              },
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "search_keys",
      "Search for translation keys in a Tolgee project by name or translation text. Returns up to 50 matching keys.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("query", "Search query (matches key name or translation text)", required = true)
        string("languageTag", "Optional: language tag to search translations in")
        string("branch", "Optional: branch name (for branching projects)")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(searchKeysSpec, projectId) {
        val results =
          keyService.searchKeys(
            search = request.arguments.getString("query") ?: "",
            languageTag = request.arguments.getString("languageTag"),
            project = projectHolder.project,
            branch = request.arguments.getString("branch"),
            pageable = PageRequest.of(0, 50),
          )
        val mapped =
          results.content.map { r ->
            mapOf(
              "keyId" to r.id,
              "keyName" to r.name,
              "keyNamespace" to r.namespace,
              "baseTranslation" to r.baseTranslation,
              "translation" to r.translation,
            )
          }
        textResult(objectMapper.writeValueAsString(mapped))
      }
    }

    server.addTool(
      "create_key",
      "Create a new translation key in a Tolgee project, optionally with initial translations and tags",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("keyName", "Key name (e.g. 'home.welcome_message')", required = true)
        string("namespace", "Optional: namespace for the key")
        stringMap("translations", "Optional: initial translations as {languageTag: text} map")
        stringArray("tags", "Optional: tags to assign to the key")
        string("description", "Optional: description / developer context for the key")
        string("branch", "Optional: branch name (for branching projects)")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(createKeySpec, projectId) {
        val args = request.arguments
        val dto =
          CreateKeyDto(
            name = args.getString("keyName") ?: "",
            namespace = args.getString("namespace"),
            translations = args.getStringMap("translations"),
            tags = args.getStringList("tags"),
            description = args.getString("description"),
            branch = args.getString("branch"),
          )
        val key = keyService.create(projectHolder.projectEntity, dto)
        val result =
          mapOf(
            "id" to key.id,
            "name" to key.name,
            "namespace" to key.namespace?.name,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "get_key",
      "Get a translation key's metadata (name, namespace, description) by its ID. To get the key's translations, use get_translations.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        number("keyId", "ID of the key", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(getKeySpec, projectId) {
        val keyId = request.arguments.getLong("keyId")!!
        val view = keyService.getView(projectId, keyId)
        val result =
          mapOf(
            "keyId" to view.id,
            "keyName" to view.name,
            "namespace" to view.namespace,
            "description" to view.description,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "update_key",
      "Update an existing translation key's name, namespace, or description",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        number("keyId", "ID of the key to update", required = true)
        string("name", "New key name (provide the current name if unchanged)", required = true)
        string("namespace", "Optional: new namespace for the key")
        string("description", "Optional: new description for the key")
        string("branch", "Optional: branch name (for branching projects)")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(editKeySpec, projectId) {
        val keyId = request.arguments.getLong("keyId")!!
        val dto =
          EditKeyDto(
            name = request.arguments.getString("name") ?: "",
            namespace = request.arguments.getString("namespace"),
            description = request.arguments.getString("description"),
            branch = request.arguments.getString("branch"),
          )
        val key = keyService.edit(keyId, dto)
        val result =
          mapOf(
            "id" to key.id,
            "name" to key.name,
            "namespace" to key.namespace?.name,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "batch_create_keys",
      "Import multiple keys with translations at once. If a key already exists, its translations and tags are not updated.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        objectArray("keys", "List of keys to import", required = true) {
          string("name", "Key name", required = true)
          string("namespace", "Optional: namespace for the key")
          stringMap("translations", "Translations as {languageTag: text} map")
          stringArray("tags", "Optional: tags for the key")
          string("description", "Optional: description of the key")
        }
        string("branch", "Optional: branch name (for branching projects)")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(importKeysSpec, projectId) {
        val branch = request.arguments.getString("branch")
        val keys =
          request.arguments.getList("keys")?.map { k ->
            ImportKeysItemDto(
              name = k.getString("name") ?: "",
              namespace = k.getString("namespace"),
              translations = k.getStringMap("translations") ?: emptyMap(),
              tags = k.getStringList("tags"),
              description = k.getString("description"),
            )
          } ?: emptyList()

        keyService.importKeys(keys, projectHolder.projectEntity, branch)
        textResult(objectMapper.writeValueAsString(mapOf("imported" to true, "keyCount" to keys.size)))
      }
    }
  }
}
