package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.batch.BatchJobService
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(BatchJobBaseConfiguration::class)
class McpBatchToolsTest : AbstractMcpTest() {
  @Autowired
  lateinit var batchJobService: BatchJobService

  lateinit var data: McpTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClient(data.pat.token!!)

    whenever(internalProperties.fakeMtProviders).thenReturn(true)
  }

  @Test
  fun `machine_translate starts a batch job`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "mt.key", "translations" to mapOf("en" to "Hello"))),
      ),
    )

    callTool(
      client,
      "create_language",
      mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
    )

    val json =
      callToolAndGetJson(
        client,
        "machine_translate",
        mapOf(
          "projectId" to data.projectId,
          "keyNames" to listOf("mt.key"),
          "targetLanguageTags" to listOf("de"),
        ),
      )
    assertThat(json["jobId"]).isNotNull()
    assertThat(json["type"].asText()).isEqualTo("MACHINE_TRANSLATE")
    assertThat(json["totalItems"].asInt()).isEqualTo(1)

    val jobDto = batchJobService.findJobDto(json["jobId"].asLong())
    assertThat(jobDto).isNotNull()
  }

  @Test
  fun `get_batch_job_status returns job status`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "status.key", "translations" to mapOf("en" to "Hi"))),
      ),
    )

    callTool(
      client,
      "create_language",
      mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
    )

    val mtResult =
      callToolAndGetJson(
        client,
        "machine_translate",
        mapOf(
          "projectId" to data.projectId,
          "keyNames" to listOf("status.key"),
          "targetLanguageTags" to listOf("de"),
        ),
      )
    val jobId = mtResult["jobId"].asLong()

    val json =
      callToolAndGetJson(
        client,
        "get_batch_job_status",
        mapOf("projectId" to data.projectId, "jobId" to jobId),
      )
    assertThat(json["id"].asLong()).isEqualTo(jobId)
    assertThat(json["status"]).isNotNull()
    assertThat(json["type"].asText()).isEqualTo("MACHINE_TRANSLATE")
    assertThat(json["totalItems"].asInt()).isEqualTo(1)
  }
}
