package io.tolgee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.data.McpEmailVerificationTestData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpEmailVerificationTest : AbstractMcpTest() {
  private lateinit var testData: McpEmailVerificationTestData

  @BeforeEach
  fun setupVerificationRequired() {
    tolgeeProperties.authentication.needsEmailVerification = true
  }

  @AfterEach
  fun cleanup() {
    tolgeeProperties.authentication.needsEmailVerification = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `tool call fails for a user with unverified email`() {
    val client = clientForUser(verified = false)
    assertToolFails(client, "list_keys", projectArgs(), expectedError = "email_not_verified")
  }

  @Test
  fun `tool call succeeds for a user with verified email`() {
    val client = clientForUser(verified = true)
    assertToolSucceeds(client, "list_keys", projectArgs())
  }

  private fun projectArgs() = mapOf<String, Any?>("projectId" to testData.project.id)

  private fun clientForUser(verified: Boolean): McpSyncClient {
    testData = McpEmailVerificationTestData(verified)
    testDataService.saveTestData(testData.root)
    return createMcpClientWithPat(testData.pat.token!!)
  }
}
