package io.tolgee.mcp.tools

import io.tolgee.api.v2.controllers.NamespaceController
import io.tolgee.api.v2.controllers.NamespacesFacade
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class NamespaceMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val namespacesFacade: NamespacesFacade,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listNamespacesSpec = buildSpec(NamespaceController::getAllNamespaces, "list_namespaces")

  override fun register(registrar: McpToolRegistrar) {
    registrar.addTool(
      "list_namespaces",
      "List namespaces",
      McpToolEffect.READ_ONLY,
      "List all namespaces in a Tolgee project. Namespaces organize translation keys into logical groups (e.g. by feature, page, or module).",
      toolSchema {
        projectId()
      },
      spec = listNamespacesSpec,
    ) { request ->
      mcpRequestContext.executeAs(listNamespacesSpec, request.arguments.getProjectId()) {
        val namespaces = namespacesFacade.getAllInProject()
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
