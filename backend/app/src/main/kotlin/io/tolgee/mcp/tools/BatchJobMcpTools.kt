package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.batch.BatchJobManagementController
import io.tolgee.api.v2.controllers.batch.StartBatchJobController
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import org.springframework.stereotype.Component

@Component
class BatchJobMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val batchJobService: BatchJobService,
  private val securityService: SecurityService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val getBatchJobSpec = buildSpec(BatchJobManagementController::get, "get_batch_job_status")
  private val machineTranslateSpec = buildSpec(StartBatchJobController::machineTranslation, "machine_translate")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "get_batch_job_status",
      "Get the status of a batch job (e.g. machine translation) by its ID. Use to poll for completion.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        number("jobId", "ID of the batch job", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(getBatchJobSpec, projectId) {
        val jobId = request.arguments.requireLong("jobId")
        val view = batchJobService.getView(jobId)
        if (view.batchJob.project?.id != projectId) {
          return@executeAs errorResult("Batch job $jobId does not belong to this project")
        }
        val result =
          mapOf(
            "id" to view.batchJob.id,
            "status" to view.batchJob.status.name,
            "type" to view.batchJob.type.name,
            "progress" to view.progress,
            "totalItems" to view.batchJob.totalItems,
            "errorMessage" to view.errorMessage?.code,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }

    server.addTool(
      "machine_translate",
      "Start machine translation for specified keys into target languages. Returns a batch job ID — use get_batch_job_status to poll for completion.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        stringArray("keyNames", "Names of keys to translate", required = true)
        stringArray("targetLanguageTags", "Language tags to translate into (e.g. ['de', 'fr'])", required = true)
        string("namespace", "Optional: namespace of the keys")
        string("branch", "Optional: branch name")
      },
    ) { request ->
      val projectId = request.arguments.getProjectId()
      mcpRequestContext.executeAs(machineTranslateSpec, projectId) {
        val keyNames = request.arguments.requireStringList("keyNames")
        val targetLanguageTags = request.arguments.requireStringList("targetLanguageTags")
        val namespace = request.arguments.getString("namespace")
        val branch = request.arguments.getString("branch")

        // Resolve key names to IDs
        val (resolvedKeys, notFoundKeys) = keyService.resolveKeysByName(projectId, keyNames, namespace, branch)
        if (notFoundKeys.isNotEmpty()) {
          return@executeAs errorResult("Keys not found: ${notFoundKeys.joinToString(", ")}")
        }
        val keyIds = resolvedKeys.map { it.id }

        // Resolve language tags to IDs
        val resolvedLangs =
          targetLanguageTags.map { tag ->
            tag to languageService.findByTag(tag, projectId)
          }
        val notFoundLangs = resolvedLangs.filter { it.second == null }.map { it.first }
        if (notFoundLangs.isNotEmpty()) {
          return@executeAs errorResult("Languages not found: ${notFoundLangs.joinToString(", ")}")
        }
        val targetLanguageIds = resolvedLangs.mapNotNull { it.second?.id }

        securityService.checkLanguageTranslatePermission(projectId, targetLanguageIds)
        securityService.checkKeyIdsExistAndIsFromProject(keyIds, projectId)

        val data = MachineTranslationRequest()
        data.keyIds = keyIds
        data.targetLanguageIds = targetLanguageIds

        val batchJob =
          batchJobService.startJob(
            data,
            projectHolder.projectEntity,
            authenticationFacade.authenticatedUserEntity,
            BatchJobType.MACHINE_TRANSLATE,
          )

        val result =
          mapOf(
            "jobId" to batchJob.id,
            "status" to batchJob.status.name,
            "type" to batchJob.type.name,
            "totalItems" to batchJob.totalItems,
          )
        textResult(objectMapper.writeValueAsString(result))
      }
    }
  }
}
