package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.batch.BatchJobManagementController
import io.tolgee.api.v2.controllers.batch.StartBatchJobController
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.mcp.McpSecurityContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.SecurityService
import org.springframework.stereotype.Component

@Component
class BatchMcpTools(
  private val mcpSecurityContext: McpSecurityContext,
  private val batchJobService: BatchJobService,
  private val securityService: SecurityService,
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
      val projectId = request.arguments.getLong("projectId")!!
      mcpSecurityContext.executeAs(getBatchJobSpec, projectId) {
        val jobId = request.arguments.getLong("jobId")!!
        val view = batchJobService.getView(jobId)
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
      "Start machine translation for specified keys into target languages. Returns a batch job ID — use get_batch_job_status to poll for completion. Use list_languages to get target language IDs.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        numberArray("keyIds", "IDs of keys to translate", required = true)
        numberArray("targetLanguageIds", "IDs of target languages to translate into", required = true)
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpSecurityContext.executeAs(machineTranslateSpec, projectId) {
        val keyIds = request.arguments.getLongList("keyIds") ?: emptyList()
        val targetLanguageIds = request.arguments.getLongList("targetLanguageIds") ?: emptyList()

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
