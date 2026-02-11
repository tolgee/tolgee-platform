package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.NamespaceController
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.NamespaceService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class NamespaceMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val namespaceService: NamespaceService,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listNamespacesSpec = buildSpec(NamespaceController::getAllNamespaces, "list_namespaces")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_namespaces",
      "List all namespaces in a Tolgee project. Namespaces organize translation keys into logical groups (e.g. by feature, page, or module).",
      toolSchema {
        number("projectId", "ID of the project", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(listNamespacesSpec, projectId) {
        val namespaces =
          namespaceService.getAllInProject(projectId)
        val result =
          namespaces.map { ns ->
            mapOf(
              "id" to ns.id,
              "name" to ns.name,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }
  }
}
