package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.keys.KeyController
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
  private val createKeysSpec =
    buildSpec(
      KeyController::class.java,
      "importKeys",
      "create_keys",
      ImportKeysDto::class.java,
      String::class.java,
    )
  private val editKeySpec = buildSpec(KeyController::edit, "update_key")
  private val getKeySpec = buildSpec(KeyController::get, "get_key")
  private val deleteKeysSpec =
    buildSpec(
      KeyController::class.java,
      "delete",
      "delete_keys",
      Set::class.java,
    )

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_keys",
      "List translation keys in a Tolgee project. " +
        "Returns up to 100 keys per page. Use search_keys to find specific keys by name or translation text.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("branch", "Optional: branch name")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      mcpRequestContext.executeAs(listKeysSpec, request.arguments.getProjectId()) {
        val pageNum = request.arguments.getInt("page") ?: 0
        val page =
          keyService.getPaged(
            projectHolder.project.id,
            request.arguments.getString("branch"),
            PageRequest.of(pageNum, 100),
          )
        val result =
          pagedResponse(page) { key ->
            mapOf(
              "keyId" to key.id,
              "keyName" to key.name,
              "keyNamespace" to key.namespace,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "search_keys",
      "Search for translation keys in a Tolgee project by name or translation text. " +
        "Returns up to 50 matching keys per page. " +
        "Note: namespace and tags filtering is not yet supported — filter results client-side if needed.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("query", "Search query (matches key name or translation text)", required = true)
        string("languageTag", "Optional: language tag to search translations in")
        string("branch", "Optional: branch name")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      mcpRequestContext.executeAs(searchKeysSpec, request.arguments.getProjectId()) {
        val pageNum = request.arguments.getInt("page") ?: 0
        val results =
          keyService.searchKeys(
            search = request.arguments.requireString("query"),
            languageTag = request.arguments.getString("languageTag"),
            project = projectHolder.project,
            branch = request.arguments.getString("branch"),
            pageable = PageRequest.of(pageNum, 50),
          )
        val result =
          pagedResponse(results) { r ->
            mapOf(
              "keyId" to r.id,
              "keyName" to r.name,
              "keyNamespace" to r.namespace,
              "baseTranslation" to r.baseTranslation,
              "translation" to r.translation,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "create_keys",
      "Create translation keys in a Tolgee project with optional translations and tags. " +
        "Keys that already exist are silently skipped — their translations and tags are not updated. " +
        "Use update_key and set_translation to modify existing keys.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        objectArray("keys", "List of keys to create", required = true) {
          string("name", "Key name (e.g. 'home.welcome_message')", required = true)
          string("namespace", "Optional: namespace for the key (overrides top-level namespace)")
          stringMap("translations", "Optional: translations as {languageTag: text} map")
          stringArray("tags", "Optional: tags to assign to the key")
          string("description", "Optional: description / developer context for the key")
        }
        string("namespace", "Optional: default namespace for all keys (individual keys can override)")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(createKeysSpec, request.arguments.getProjectId()) {
        val branch = request.arguments.getString("branch")
        val defaultNamespace = request.arguments.getString("namespace")
        val keys =
          request.arguments.requireList("keys").map { k ->
            ImportKeysItemDto(
              name = k.requireString("name"),
              namespace = k.getString("namespace") ?: defaultNamespace,
              translations = k.getStringMap("translations") ?: emptyMap(),
              tags = k.getStringList("tags"),
              description = k.getString("description"),
            )
          }

        keyService.importKeys(keys, projectHolder.projectEntity, branch)
        textResult(objectMapper.writeValueAsString(mapOf("created" to true, "keyCount" to keys.size)))
      }
    }

    server.addTool(
      "get_key",
      "Get a translation key's metadata (name, namespace, description) by its name. To get the key's translations, use get_translations.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "The translation key name", required = true)
        string("namespace", "Optional: namespace of the key")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(getKeySpec, request.arguments.getProjectId()) {
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
          val result =
            mapOf(
              "keyId" to key.id,
              "keyName" to key.name,
              "namespace" to key.namespace?.name,
              "description" to key.keyMeta?.description,
            )
          textResult(objectMapper.writeValueAsString(result))
        }
      }
    }

    server.addTool(
      "update_key",
      "Update an existing translation key's name, namespace, or description",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("keyName", "Current key name (used to find the key)", required = true)
        string("keyNamespace", "Optional: current namespace of the key (used to find the key)")
        string("keyBranch", "Optional: branch name (used to find the key, for branching projects)")
        string("newName", "New key name (provide the current name if unchanged)", required = true)
        string("newNamespace", "Optional: new namespace for the key")
        string("newDescription", "Optional: new description for the key")
      },
    ) { request ->
      mcpRequestContext.executeAs(editKeySpec, request.arguments.getProjectId()) {
        val key =
          keyService.find(
            projectId = projectHolder.project.id,
            name = request.arguments.requireString("keyName"),
            namespace = request.arguments.getString("keyNamespace"),
            branch = request.arguments.getString("keyBranch"),
          )
        if (key == null) {
          errorResult("Key not found")
        } else {
          val dto =
            EditKeyDto(
              name = request.arguments.requireString("newName"),
              namespace = request.arguments.getString("newNamespace"),
              description = request.arguments.getString("newDescription"),
            )
          val updated = keyService.edit(key.id, dto)
          val result =
            mapOf(
              "id" to updated.id,
              "name" to updated.name,
              "namespace" to updated.namespace?.name,
            )
          textResult(objectMapper.writeValueAsString(result))
        }
      }
    }

    server.addTool(
      "delete_keys",
      "Delete translation keys from a Tolgee project. " +
        "IMPORTANT: This is a destructive operation. The AI assistant should always confirm with the user before calling this tool.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        stringArray("keyNames", "Names of the keys to delete", required = true)
        string("namespace", "Optional: namespace of the keys")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(deleteKeysSpec, request.arguments.getProjectId()) {
        val keyNames = request.arguments.requireStringList("keyNames")
        val namespace = request.arguments.getString("namespace")
        val branch = request.arguments.getString("branch")

        val (keys, notFound) = keyService.resolveKeysByName(projectHolder.project.id, keyNames, namespace, branch)
        if (notFound.isNotEmpty()) {
          errorResult("Keys not found: ${notFound.joinToString(", ")}")
        } else {
          val ids = keys.map { it.id }.toSet()
          keyService.deleteMultiple(ids)
          textResult(
            objectMapper.writeValueAsString(
              mapOf("deleted" to true, "keyCount" to ids.size),
            ),
          )
        }
      }
    }
  }
}
