package io.tolgee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.Scope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * get_batch_job_status must enforce the same rules as the REST batch-job endpoint.
 * See GHSA-j998-54vh-rwpg.
 */
class McpBatchJobPermissionsTest : AbstractMcpTest() {
  @Autowired
  lateinit var batchJobService: BatchJobService

  private lateinit var testData: BatchJobsTestData
  private var projectKeyId: Long = 0
  private var unrelatedKeyId: Long = 0

  @BeforeEach
  fun setup() {
    testData = BatchJobsTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  private fun client(vararg scopes: Scope): McpSyncClient {
    var apiKey: ApiKey? = null
    testData.projectBuilder.addApiKey {
      key = "batch_permissions_test_pak"
      userAccount = testData.user
      scopesEnum = scopes.toMutableSet()
      apiKey = this
    }
    addJobKeysAndSave()
    return createMcpClientWithPak(apiKey!!.encodedKey!!)
  }

  private fun patClientForAnotherUser(): McpSyncClient {
    var pat: Pat? = null
    testData.root.data.userAccounts
      .single { it.self.username == "anotherUser" }
      .build {
        addPat {
          description = "MCP batch test PAT"
          pat = this
        }
      }
    addJobKeysAndSave()
    return createMcpClientWithPat(pat!!.token!!)
  }

  private fun addJobKeysAndSave() {
    val projectKey = testData.projectBuilder.addKey { name = "job-key" }.self
    val unrelatedKey = testData.unrelatedProject.addKey { name = "unrelated-job-key" }.self
    testDataService.saveTestData(testData.root)
    projectKeyId = projectKey.id
    unrelatedKeyId = unrelatedKey.id
  }

  private fun startJob(
    project: Project,
    author: UserAccount,
    keyId: Long,
  ): BatchJob =
    executeInNewTransaction {
      batchJobService.startJob(
        request = DeleteKeysRequest().apply { keyIds = listOf(keyId) },
        project = project,
        author = author,
        type = BatchJobType.DELETE_KEYS,
      )
    }

  @Test
  fun `get_batch_job_status hides another project's job like a nonexistent one`() {
    val mcp = client(Scope.BATCH_JOBS_VIEW)
    val foreignJob = startJob(testData.unrelatedProject.self, testData.anotherUser, unrelatedKeyId)

    assertToolFails(
      mcp,
      "get_batch_job_status",
      mapOf("jobId" to foreignJob.id),
      expectedError = "batch_job_not_found",
    )
    assertToolFails(
      mcp,
      "get_batch_job_status",
      mapOf("jobId" to 1234567890L),
      expectedError = "batch_job_not_found",
    )
  }

  @Test
  fun `get_batch_job_status denies another author's job without BATCH_JOBS_VIEW`() {
    val mcp = client(Scope.TRANSLATIONS_VIEW)
    val job = startJob(testData.projectBuilder.self, testData.anotherUser, projectKeyId)

    assertToolFails(mcp, "get_batch_job_status", mapOf("jobId" to job.id))
  }

  @Test
  fun `get_batch_job_status allows the job author without BATCH_JOBS_VIEW`() {
    val mcp = patClientForAnotherUser()
    val job = startJob(testData.projectBuilder.self, testData.anotherUser, projectKeyId)

    assertToolSucceeds(
      mcp,
      "get_batch_job_status",
      mapOf("projectId" to testData.projectBuilder.self.id, "jobId" to job.id),
    )
  }
}
