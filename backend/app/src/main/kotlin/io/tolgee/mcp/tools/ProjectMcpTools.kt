package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.ProjectStatsController
import io.tolgee.api.v2.controllers.project.ProjectsController
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.project.ProjectStatsService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProjectMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val projectService: ProjectService,
  private val projectCreationService: ProjectCreationService,
  private val projectStatsService: ProjectStatsService,
  private val organizationRoleService: OrganizationRoleService,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listProjectsSpec = buildSpec(ProjectsController::getAll, "list_projects")
  private val createProjectSpec = buildSpec(ProjectsController::createProject, "create_project")
  private val getProjectStatsSpec = buildSpec(ProjectStatsController::getProjectStats, "get_project_stats")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "list_projects",
      "List Tolgee projects accessible to the current user (up to 100 results)",
      toolSchema {
        string("search", "Optional search query to filter projects by name")
      },
    ) { request ->
      mcpRequestContext.executeAs(listProjectsSpec) {
        val search = request.arguments?.getString("search")
        val projects =
          projectService.findPermittedInOrganizationPaged(
            PageRequest.of(0, 100),
            search,
          )
        val result =
          projects.content.map { p ->
            mapOf(
              "id" to p.id,
              "name" to p.name,
              "slug" to p.slug,
              "description" to p.description,
              "organizationOwnerName" to p.organizationOwner.name,
              "organizationOwnerSlug" to p.organizationOwner.slug,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "create_project",
      "Create a new Tolgee project with initial languages",
      toolSchema {
        string("name", "Project name", required = true)
        number("organizationId", "ID of the organization to create the project in", required = true)
        objectArray("languages", "Initial languages for the project", required = true) {
          string("name", "Language name (e.g. 'English')", required = true)
          string("tag", "Language tag (e.g. 'en')", required = true)
          string("originalName", "Original name of the language")
          string("flagEmoji", "Flag emoji for the language")
        }
        string("baseLanguageTag", "Tag of the base language (default: first language)")
      },
    ) { request ->
      mcpRequestContext.executeAs(createProjectSpec) {
        val args = request.arguments
        val languages =
          args.getList("languages")?.map { lang ->
            LanguageRequest(
              name = lang.getString("name") ?: "",
              tag = lang.getString("tag") ?: "",
              originalName = lang.getString("originalName"),
              flagEmoji = lang.getString("flagEmoji"),
            )
          }

        val dto =
          CreateProjectRequest(
            name = args.getString("name") ?: "",
            organizationId = args.getLong("organizationId") ?: 0,
            languages = languages,
            baseLanguageTag = args.getString("baseLanguageTag"),
          )

        organizationRoleService.checkUserCanCreateProject(dto.organizationId)
        val project = projectCreationService.createProject(dto)
        val result =
          mapOf(
            "id" to project.id,
            "name" to project.name,
            "slug" to project.slug,
            "description" to project.description,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "get_project_stats",
      "Get project statistics including key count, member count, tag count, and task count",
      toolSchema {
        number("projectId", "ID of the project", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(getProjectStatsSpec, projectId) {
        val stats = projectStatsService.getProjectStats(projectId)
        val result =
          mapOf(
            "projectId" to stats.id,
            "keyCount" to stats.keyCount,
            "memberCount" to stats.memberCount,
            "tagCount" to stats.tagCount,
            "taskCount" to stats.taskCount,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }
  }
}
