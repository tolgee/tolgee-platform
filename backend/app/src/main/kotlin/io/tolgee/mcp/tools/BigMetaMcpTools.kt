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
import org.springframework.stereotype.Component

@Component
class BigMetaMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val bigMetaService: BigMetaService,
  private val projectHolder: ProjectHolder,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  private val storeBigMetaSpec = buildSpec(BigMetaController::store, "store_big_meta")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "store_big_meta",
      "Store key relationships so Tolgee can use translations of related keys as context during machine translation, " +
        "producing more consistent results. Keys that appear near each other in source code " +
        "(e.g. on the same page or component) should be stored as related.",
      toolSchema {
        number("projectId", "ID of the project", required = true)
        objectArray("relatedKeysInOrder", "List of related keys in the order they appear together", required = true) {
          string("keyName", "Key name", required = true)
          string("namespace", "Optional: key namespace")
        }
        string("branch", "Optional: branch name (for branching projects)")
      },
    ) { request ->
      val projectId = request.arguments.getLong("projectId")!!
      mcpRequestContext.executeAs(storeBigMetaSpec, projectId) {
        val branch = request.arguments.getString("branch")
        val relatedKeys =
          request.arguments
            .getList("relatedKeysInOrder")
            ?.map { k ->
              RelatedKeyDto(
                keyName = k.getString("keyName") ?: "",
                namespace = k.getString("namespace"),
                branch = branch,
              )
            }?.toMutableList()

        val dto = BigMetaDto()
        dto.relatedKeysInOrder = relatedKeys
        bigMetaService.store(dto, projectHolder.projectEntity)
        textResult(objectMapper.writeValueAsString(mapOf("stored" to true, "keyCount" to (relatedKeys?.size ?: 0))))
      }
    }
  }
}
