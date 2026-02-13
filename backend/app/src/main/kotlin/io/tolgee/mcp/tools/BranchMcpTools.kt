package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.constants.Feature
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.branching.BranchService
import io.tolgee.service.project.ProjectFeatureGuard
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
@ConditionalOnClass(name = ["io.tolgee.ee.service.branching.BranchServiceImpl"])
class BranchMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val branchService: BranchService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  companion object {
    private val branchControllerClass =
      Class.forName("io.tolgee.ee.api.v2.controllers.branching.BranchController")
    private val createBranchModelClass =
      Class.forName("io.tolgee.ee.api.v2.hateoas.model.branching.CreateBranchModel")
  }

  private val listBranchesSpec =
    buildSpec(branchControllerClass, "all", "list_branches", Pageable::class.java, String::class.java)
  private val createBranchSpec =
    buildSpec(branchControllerClass, "create", "create_branch", createBranchModelClass)
  private val deleteBranchSpec =
    buildSpec(branchControllerClass, "delete", "delete_branch", Long::class.javaPrimitiveType!!)

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_branches",
      "List branches in a Tolgee project. Returns up to 100 results per page. Only available for projects with branching enabled (enterprise feature).",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("search", "Optional: search filter for branch names")
        number("page", "Optional: page number (0-based, default 0)")
      },
    ) { request ->
      mcpRequestContext.executeAs(listBranchesSpec, request.arguments.getProjectId()) {
        projectFeatureGuard.checkEnabled(Feature.BRANCHING)
        val search = request.arguments.getString("search")
        val pageNum = request.arguments.getInt("page") ?: 0
        val page =
          branchService.getBranches(
            projectId = projectHolder.project.id,
            page = PageRequest.of(pageNum, 100, Sort.by("id")),
            search = search,
          )
        val result =
          pagedResponse(page) { branch ->
            mapOf(
              "id" to branch.id,
              "name" to branch.name,
              "isDefault" to branch.isDefault,
              "isProtected" to branch.isProtected,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "create_branch",
      "Create a new branch in a Tolgee project by forking from an existing branch. Only available for projects with branching enabled (enterprise feature).",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("name", "Name of the new branch", required = true)
        number("originBranchId", "ID of the branch to fork from", required = true)
      },
    ) { request ->
      mcpRequestContext.executeAs(createBranchSpec, request.arguments.getProjectId()) {
        projectFeatureGuard.checkEnabled(Feature.BRANCHING)
        val name = request.arguments.requireString("name")
        val originBranchId = request.arguments.requireLong("originBranchId")
        val branch =
          branchService.createBranch(
            projectId = projectHolder.project.id,
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
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        string("branchName", "Name of the branch to delete", required = true)
      },
    ) { request ->
      mcpRequestContext.executeAs(deleteBranchSpec, request.arguments.getProjectId()) {
        projectFeatureGuard.checkEnabled(Feature.BRANCHING)
        val branchName = request.arguments.requireString("branchName")
        val branch = branchService.getActiveBranch(projectHolder.project.id, branchName)
        branchService.deleteBranch(projectHolder.project.id, branch.id)
        textResult(objectMapper.writeValueAsString(mapOf("deleted" to true)))
      }
    }
  }
}
