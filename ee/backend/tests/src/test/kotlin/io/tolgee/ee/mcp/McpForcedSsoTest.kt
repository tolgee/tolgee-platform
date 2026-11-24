package io.tolgee.ee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.data.McpForcedSsoTestData
import io.tolgee.model.enums.ThirdPartyAuthType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpForcedSsoTest : AbstractMcpTest() {
  private lateinit var testData: McpForcedSsoTestData

  @BeforeEach
  fun enableSsoOrganizations() {
    tolgeeProperties.authentication.ssoOrganizations.enabled = true
  }

  @AfterEach
  fun cleanup() {
    tolgeeProperties.authentication.ssoOrganizations.enabled = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `tool call fails for a non-SSO user whose domain forces SSO`() {
    val client = clientForUser("user@forced.example.com", forcedDomain = "forced.example.com")
    assertToolFails(client, "list_keys", projectArgs(), expectedError = "sso_login_forced_for_this_account")
  }

  @Test
  fun `tool call succeeds for a user whose domain does not force SSO`() {
    val client = clientForUser("user@open.example.com", forcedDomain = null)
    assertToolSucceeds(client, "list_keys", projectArgs())
  }

  @Test
  fun `tool call succeeds for an SSO user whose domain forces SSO`() {
    val client =
      clientForUser(
        "sso-user@forced.example.com",
        forcedDomain = "forced.example.com",
        thirdPartyAuth = ThirdPartyAuthType.SSO,
      )
    assertToolSucceeds(client, "list_keys", projectArgs())
  }

  private fun clientForUser(
    userName: String,
    forcedDomain: String?,
    thirdPartyAuth: ThirdPartyAuthType? = null,
  ): McpSyncClient {
    testData = McpForcedSsoTestData(userName, forcedDomain, thirdPartyAuth)
    testDataService.saveTestData(testData.root)
    return createMcpClientWithPat(testData.pat.token!!)
  }

  private fun projectArgs() = mapOf<String, Any?>("projectId" to testData.project.id)
}
