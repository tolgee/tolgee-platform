package io.tolgee.mcp.tools

import io.tolgee.api.v2.controllers.ProjectStatsController
import io.tolgee.api.v2.controllers.ProjectStatsFacade
import io.tolgee.api.v2.controllers.project.ProjectsController
import io.tolgee.api.v2.controllers.project.ProjectsFacade
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.util.executeInNewTransaction
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.ObjectMapper

@Component
class ProjectMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val projectsFacade: ProjectsFacade,
  private val projectStatsFacade: ProjectStatsFacade,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : McpToolsProvider {
  private val listProjectsSpec =
    buildSpec(ProjectsController::getAll, "list_projects").copy(allowsOAuthToken = true)
  private val createProjectSpec = buildSpec(ProjectsController::createProject, "create_project")
  private val getLanguageStatsSpec =
    buildSpec(ProjectStatsController::getProjectStats, "get_project_language_statistics")

  override fun register(registrar: McpToolRegistrar) {
    registrar.addTool(
      "list_projects",
      "List projects",
      McpToolEffect.READ_ONLY,
      "List Tolgee projects accessible to the current user. An OAuth token lists only the projects it was " +
        "authorized for. Returns up to 100 results per page.",
      toolSchema {
        string("search", "Optional search query to filter projects by name")
        number("page", "Optional: page number (0-based, default 0)")
      },
      spec = listProjectsSpec,
    ) { request ->
      mcpRequestContext.executeAs(listProjectsSpec) {
        val search = request.arguments?.getString("search")
        val pageNum = request.arguments?.getInt("page") ?: 0
        val projects = projectsFacade.findPermitted(PageRequest.of(pageNum, 100), search)
        val result =
          pagedResponse(projects) { p ->
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

    registrar.addTool(
      "create_project",
      "Create project",
      McpToolEffect.ADDITIVE,
      "Create a new Tolgee project with initial languages",
      toolSchema {
        string("name", "Project name", required = true)
        number("organizationId", "ID of the organization to create the project in", required = true)
        objectArray("languages", "Initial languages for the project", required = true) {
          string("name", "Language name (e.g. 'English')", required = true)
          string("tag", "Language tag (e.g. 'en')", required = true)
          string("originalName", "Original name of the language. Defaults to the value of 'name'.")
          string("flagEmoji", "Flag emoji for the language")
        }
        string("baseLanguageTag", "Tag of the base language (default: first language)")
      },
      spec = createProjectSpec,
    ) { request ->
      mcpRequestContext.executeAs(createProjectSpec) {
        val args = request.arguments
        val languages =
          args.requireList("languages").map { lang ->
            LanguageRequest(
              name = lang.requireString("name"),
              tag = lang.requireString("tag"),
              originalName = lang.getString("originalName") ?: lang.requireString("name"),
              flagEmoji = lang.getString("flagEmoji"),
            )
          }

        val dto =
          CreateProjectRequest(
            name = args.requireString("name"),
            organizationId = args.requireLong("organizationId"),
            languages = languages,
            baseLanguageTag = args.getString("baseLanguageTag"),
          )

        executeInNewTransaction(transactionManager) {
          val project = projectsFacade.createProject(dto)
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
    }

    registrar.addTool(
      "get_project_language_statistics",
      "Get project language statistics",
      McpToolEffect.READ_ONLY,
      "Get translation status and progress for each language in a project. " +
        "Returns per-language statistics including translated, reviewed, and untranslated percentages.",
      toolSchema {
        projectId()
      },
      spec = getLanguageStatsSpec,
    ) { request ->
      mcpRequestContext.executeAs(getLanguageStatsSpec, request.arguments.getProjectId()) {
        val result =
          projectStatsFacade.getLanguageStats().map { (lang, stat) ->
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
