package io.tolgee.mcp.tools

import io.tolgee.api.v2.controllers.LanguagesFacade
import io.tolgee.api.v2.controllers.batch.BatchJobManagementController
import io.tolgee.api.v2.controllers.batch.BatchJobsFacade
import io.tolgee.api.v2.controllers.batch.StartBatchJobController
import io.tolgee.api.v2.controllers.keys.KeyFacade
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.util.executeInNewTransaction
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.ObjectMapper

@Component
class BatchJobMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val batchJobsFacade: BatchJobsFacade,
  private val keyFacade: KeyFacade,
  private val languagesFacade: LanguagesFacade,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : McpToolsProvider {
  private val getBatchJobSpec = buildSpec(BatchJobManagementController::get, "get_batch_job_status")
  private val machineTranslateSpec = buildSpec(StartBatchJobController::machineTranslation, "machine_translate")

  override fun register(registrar: McpToolRegistrar) {
    registrar.addTool(
      "get_batch_job_status",
      "Get batch job status",
      McpToolEffect.READ_ONLY,
      "Get the status of a batch job (e.g. machine translation) by its ID. Use to poll for completion.",
      toolSchema {
        projectId()
        number("jobId", "ID of the batch job", required = true)
      },
      spec = getBatchJobSpec,
    ) { request ->
      mcpRequestContext.executeAs(getBatchJobSpec, request.arguments.getProjectId()) {
        val jobId = request.arguments.requireLong("jobId")
        executeInNewTransaction(transactionManager) {
          val view = batchJobsFacade.getView(jobId)
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
    }

    registrar.addTool(
      "machine_translate",
      "Machine translate keys",
      McpToolEffect.DESTRUCTIVE,
      "Start machine translation for specified keys into target languages. Returns a batch job ID — use get_batch_job_status to poll for completion.",
      toolSchema {
        projectId()
        stringArray("keyNames", "Names of keys to translate", required = true)
        stringArray("targetLanguageTags", "Language tags to translate into (e.g. ['de', 'fr'])", required = true)
        string("namespace", "Optional: namespace of the keys")
        string("branch", "Optional: branch name")
      },
      openWorld = true,
      spec = machineTranslateSpec,
    ) { request ->
      mcpRequestContext.executeAs(machineTranslateSpec, request.arguments.getProjectId()) {
        val keyNames = request.arguments.requireStringList("keyNames")
        val targetLanguageTags = request.arguments.requireStringList("targetLanguageTags")
        val namespace = request.arguments.getString("namespace")
        val branch = request.arguments.getString("branch")

        executeInNewTransaction(transactionManager) {
          val (resolvedKeys, notFoundKeys) = keyFacade.resolveKeysByName(keyNames, namespace, branch)
          if (notFoundKeys.isNotEmpty()) {
            return@executeInNewTransaction errorResult("Keys not found: ${notFoundKeys.joinToString(", ")}")
          }

          val resolvedLangs = targetLanguageTags.map { tag -> tag to languagesFacade.findByTag(tag) }
          val notFoundLangs = resolvedLangs.filter { it.second == null }.map { it.first }
          if (notFoundLangs.isNotEmpty()) {
            return@executeInNewTransaction errorResult("Languages not found: ${notFoundLangs.joinToString(", ")}")
          }

          val data = MachineTranslationRequest()
          data.keyIds = resolvedKeys.map { it.id }
          data.targetLanguageIds = resolvedLangs.mapNotNull { it.second?.id }

          val batchJob = batchJobsFacade.startMachineTranslation(data)

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
}
