package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
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
    val createKeyResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf(
          "projectId" to data.projectId,
          "keyName" to "mt.key",
          "translations" to mapOf("en" to "Hello"),
        ),
      )
    val keyId = createKeyResult["id"].asLong()

    val createLangResult =
      callToolAndGetJson(
        client,
        "create_language",
        mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
      )
    val germanLangId = createLangResult["id"].asLong()

    val json =
      callToolAndGetJson(
        client,
        "machine_translate",
        mapOf(
          "projectId" to data.projectId,
          "keyIds" to listOf(keyId),
          "targetLanguageIds" to listOf(germanLangId),
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
    val createKeyResult =
      callToolAndGetJson(
        client,
        "create_key",
        mapOf(
          "projectId" to data.projectId,
          "keyName" to "status.key",
          "translations" to mapOf("en" to "Hi"),
        ),
      )
    val keyId = createKeyResult["id"].asLong()

    val createLangResult =
      callToolAndGetJson(
        client,
        "create_language",
        mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
      )
    val germanLangId = createLangResult["id"].asLong()

    val mtResult =
      callToolAndGetJson(
        client,
        "machine_translate",
        mapOf(
          "projectId" to data.projectId,
          "keyIds" to listOf(keyId),
          "targetLanguageIds" to listOf(germanLangId),
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
