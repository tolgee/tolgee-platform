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
import io.tolgee.service.language.LanguageService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProjectMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val projectService: ProjectService,
  private val projectCreationService: ProjectCreationService,
  private val languageStatsService: LanguageStatsService,
  private val languageService: LanguageService,
  private val organizationRoleService: OrganizationRoleService,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val listProjectsSpec = buildSpec(ProjectsController::getAll, "list_projects")
  private val createProjectSpec = buildSpec(ProjectsController::createProject, "create_project")
  private val getLanguageStatsSpec =
    buildSpec(ProjectStatsController::getProjectStats, "get_project_language_statistics")

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
      "get_project_language_statistics",
      "Get translation status and progress for each language in a project. " +
        "Returns per-language statistics including translated, reviewed, and untranslated percentages.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(getLanguageStatsSpec, projectId) {
        val languages = languageService.findAll(projectId)
        val languageIds = languages.map { it.id }.toSet()
        val stats = languageStatsService.getLanguageStats(projectId, languageIds)
        val languageById = languages.associateBy { it.id }

        val result =
          stats.map { stat ->
            val lang = languageById[stat.languageId]
            mapOf(
              "languageTag" to lang?.tag,
              "languageName" to lang?.name,
              "translatedPercentage" to stat.translatedPercentage,
              "reviewedPercentage" to stat.reviewedPercentage,
              "untranslatedPercentage" to stat.untranslatedPercentage,
              "translatedKeys" to stat.translatedKeys,
              "reviewedKeys" to stat.reviewedKeys,
              "untranslatedKeys" to stat.untranslatedKeys,
            )
          }
        textResult(objectMapper.writeValueAsString(result))
      }
    }
  }
}
