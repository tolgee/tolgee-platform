package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Feature
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.ToolEndpointSpec
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.branching.BranchService
import io.tolgee.service.project.ProjectFeatureGuard
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
@ConditionalOnClass(name = ["io.tolgee.ee.service.branching.BranchServiceImpl"])
class BranchMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val branchService: BranchService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val authenticationFacade: AuthenticationFacade,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listBranchesSpec =
    ToolEndpointSpec(
      mcpOperation = "list_branches",
      requiredScopes = null,
      useDefaultPermissions = true,
      isGlobalRoute = false,
      requiredOrgRole = null,
      requiredFeatures = arrayOf(Feature.BRANCHING),
      requiredOneOfFeatures = null,
      activityType = null,
      rateLimitPolicy = null,
      allowedTokenType = AuthTokenType.ONLY_PAT,
      isWriteOperation = false,
    )

  private val createBranchSpec =
    ToolEndpointSpec(
      mcpOperation = "create_branch",
      requiredScopes = arrayOf(Scope.BRANCH_MANAGEMENT),
      useDefaultPermissions = false,
      isGlobalRoute = false,
      requiredOrgRole = null,
      requiredFeatures = arrayOf(Feature.BRANCHING),
      requiredOneOfFeatures = null,
      activityType = ActivityType.BRANCH_CREATE,
      rateLimitPolicy = null,
      allowedTokenType = AuthTokenType.ONLY_PAT,
      isWriteOperation = true,
    )

  private val deleteBranchSpec =
    ToolEndpointSpec(
      mcpOperation = "delete_branch",
      requiredScopes = arrayOf(Scope.BRANCH_MANAGEMENT),
      useDefaultPermissions = false,
      isGlobalRoute = false,
      requiredOrgRole = null,
      requiredFeatures = arrayOf(Feature.BRANCHING),
      requiredOneOfFeatures = null,
      activityType = ActivityType.BRANCH_DELETE,
      rateLimitPolicy = null,
      allowedTokenType = AuthTokenType.ONLY_PAT,
      isWriteOperation = true,
    )

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_branches",
      "List branches in a Tolgee project. Returns up to 100 results per page. Only available for projects with branching enabled (enterprise feature).",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("search", "Optional: search filter for branch names")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(listBranchesSpec, projectId) {
        projectFeatureGuard.checkEnabled(Feature.BRANCHING)
        val search = request.arguments.getString("search")
        val pageNum = request.arguments.getInt("page") ?: 0
        val page =
          branchService.getBranches(
            projectId = projectId,
            page = PageRequest.of(pageNum, 100, Sort.by("id")),
            search = search,
          )
        val result =
          mapOf(
            "items" to
              page.content.map { branch ->
                mapOf(
                  "id" to branch.id,
                  "name" to branch.name,
                  "isDefault" to branch.isDefault,
                  "isProtected" to branch.isProtected,
                )
              },
            "page" to page.number,
            "totalPages" to page.totalPages,
            "totalItems" to page.totalElements,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "create_branch",
      "Create a new branch in a Tolgee project by forking from an existing branch. Only available for projects with branching enabled (enterprise feature).",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        string("name", "Name of the new branch", required = true)
        number("originBranchId", "ID of the branch to fork from", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(createBranchSpec, projectId) {
        projectFeatureGuard.checkEnabled(Feature.BRANCHING)
        val name = request.arguments.requireString("name")
        val originBranchId = request.arguments.requireLong("originBranchId")
        val branch =
          branchService.createBranch(
            projectId = projectId,
            name = name,
            originBranchId = originBranchId,
            author = authenticationFacade.authenticatedUserEntity,
          )
        val result =
          mapOf(
            "id" to branch.id,
            "name" to branch.name,
            "isDefault" to branch.isDefault,
            "isProtected" to branch.isProtected,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "delete_branch",
      "Delete a branch from a Tolgee project. " +
        "IMPORTANT: This is a destructive operation. The AI assistant should always confirm with the user before calling this tool.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        number("branchId", "ID of the branch to delete", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(deleteBranchSpec, projectId) {
        projectFeatureGuard.checkEnabled(Feature.BRANCHING)
        val branchId = request.arguments.requireLong("branchId")
        branchService.deleteBranch(projectId, branchId)
        textResult(objectMapper.writeValueAsString(mapOf("deleted" to true)))
      }
    }
  }
}
