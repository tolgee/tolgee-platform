package io.tolgee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.ApiKey
import io.tolgee.model.enums.Scope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * delete_keys must respect branch protection like the REST delete endpoint.
 * See GHSA-j998-54vh-rwpg.
 */
class McpBranchProtectionTest : AbstractMcpTest() {
  @MockitoBean
  @Autowired
  lateinit var enabledFeaturesProvider: EnabledFeaturesProvider

  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    doReturn(arrayOf(Feature.BRANCHING)).whenever(enabledFeaturesProvider).get(anyOrNull())
    doReturn(true).whenever(enabledFeaturesProvider).isFeatureEnabled(anyOrNull(), any())
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  private fun clientWithScopes(vararg scopes: Scope): McpSyncClient {
    testData = BaseTestData(userName = "branch_test_user", projectName = "branch_test_project")
    testData.projectBuilder.self.useBranching = true
    var apiKey: ApiKey? = null
    testData.projectBuilder.build {
      addApiKey {
        key = "branch_protection_test_pak"
        scopesEnum = scopes.toMutableSet()
        userAccount = testData.userAccountBuilder.self
        apiKey = this
      }
      addKey { name = "protected.key" }
    }
    testData.projectBuilder.addBranch {
      name = "main"
      isDefault = true
      isProtected = true
    }
    testDataService.saveTestData(testData.root)
    return createMcpClientWithPak(apiKey!!.encodedKey!!)
  }

  @Test
  fun `delete_keys denies deletion on a protected branch without BRANCH_PROTECTED_MODIFY`() {
    val client = clientWithScopes(Scope.KEYS_DELETE)

    assertToolFails(
      client,
      "delete_keys",
      mapOf("keyNames" to listOf("protected.key")),
      expectedError = "operation_not_permitted",
    )
  }

  @Test
  fun `delete_keys allows deletion on a protected branch with BRANCH_PROTECTED_MODIFY`() {
    val client = clientWithScopes(Scope.KEYS_DELETE, Scope.BRANCH_PROTECTED_MODIFY)

    assertToolSucceeds(client, "delete_keys", mapOf("keyNames" to listOf("protected.key")))
  }
}
