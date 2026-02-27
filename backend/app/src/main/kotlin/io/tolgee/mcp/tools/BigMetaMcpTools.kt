package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.BigMetaController
import io.tolgee.dtos.BigMetaDto
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.util.executeInNewTransaction
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

@Component
class BigMetaMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val bigMetaService: BigMetaService,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : McpToolsProvider {
  private val storeBigMetaSpec = buildSpec(BigMetaController::store, "store_big_meta")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "store_big_meta",
      "Store key relationships so Tolgee can use translations of related keys as context during machine translation, " +
        "producing more consistent results. Keys that appear near each other in source code " +
        "(e.g. on the same page or component) should be stored as related.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        objectArray("relatedKeysInOrder", "List of related keys in the order they appear together", required = true) {
          string("keyName", "Key name", required = true)
          string("namespace", "Optional: key namespace")
        }
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(storeBigMetaSpec, request.arguments.getProjectId()) {
        val branch = request.arguments.getString("branch")
        val relatedKeys =
          request.arguments
            .requireList("relatedKeysInOrder")
            .map { k ->
              RelatedKeyDto(
                keyName = k.requireString("keyName"),
                namespace = k.getString("namespace"),
                branch = branch,
              )
            }.toMutableList()

        val dto = BigMetaDto()
        dto.relatedKeysInOrder = relatedKeys
        executeInNewTransaction(transactionManager) {
          bigMetaService.store(dto, projectHolder.projectEntity)
          textResult(objectMapper.writeValueAsString(mapOf("stored" to true, "keyCount" to relatedKeys.size)))
        }
      }
    }
  }
}
