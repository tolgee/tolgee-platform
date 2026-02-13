package io.tolgee.ee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.service.branching.BranchService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class McpBranchToolsTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  lateinit var branchService: BranchService

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    data.testData.projectBuilder.self.useBranching = true
    testDataService.saveTestData(data.testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    client = createMcpClientWithPak(data.apiKey.encodedKey!!)
  }

  @Test
  fun `list_branches auto-resolves projectId from PAK`() {
    val json = callToolAndGetJson(client, "list_branches")
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["totalItems"].asLong()).isGreaterThanOrEqualTo(1)
    val branchNames = (0 until json["items"].size()).map { json["items"][it]["name"].asText() }
    assertThat(branchNames).contains("main")
  }

  @Test
  fun `branch tools fail when branching feature is not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    assertThat(
      org.junit.jupiter.api
        .assertThrows<io.modelcontextprotocol.spec.McpError> {
          callTool(client, "list_branches", mapOf("projectId" to data.projectId))
        }.message,
    ).contains("feature_not_enabled")
  }

  @Test
  fun `list_branches returns project branches`() {
    val json = callToolAndGetJson(client, "list_branches", mapOf("projectId" to data.projectId))
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["totalItems"].asLong()).isGreaterThanOrEqualTo(1)
    val branchNames = (0 until json["items"].size()).map { json["items"][it]["name"].asText() }
    assertThat(branchNames).contains("main")
  }

  @Test
  fun `create_branch creates a new branch`() {
    val mainBranch = branchService.getDefaultBranch(data.projectId)!!

    val json =
      callToolAndGetJson(
        client,
        "create_branch",
        mapOf(
          "projectId" to data.projectId,
          "name" to "feature-branch",
          "originBranchId" to mainBranch.id,
        ),
      )
    assertThat(json["id"].asLong()).isGreaterThan(0)
    assertThat(json["name"].asText()).isEqualTo("feature-branch")
    assertThat(json["isDefault"].asBoolean()).isFalse()

    // Verify via service
    val branch = branchService.getActiveBranch(data.projectId, "feature-branch")
    assertThat(branch.name).isEqualTo("feature-branch")
    assertThat(branch.isDefault).isFalse()
  }

  @Test
  fun `delete_branch deletes a branch`() {
    val mainBranch = branchService.getDefaultBranch(data.projectId)!!

    // First create a branch to delete
    val created =
      callToolAndGetJson(
        client,
        "create_branch",
        mapOf(
          "projectId" to data.projectId,
          "name" to "to-delete",
          "originBranchId" to mainBranch.id,
        ),
      )
    val branchId = created["id"].asLong()

    // Verify it exists via service
    val allBefore = branchService.getBranches(data.projectId, PageRequest.of(0, 100))
    assertThat(allBefore.content.map { it.name }).contains("to-delete")

    // Delete it
    val json =
      callToolAndGetJson(
        client,
        "delete_branch",
        mapOf("projectId" to data.projectId, "branchName" to "to-delete"),
      )
    assertThat(json["deleted"].asBoolean()).isTrue()

    // Verify it's gone via service
    val allAfter = branchService.getBranches(data.projectId, PageRequest.of(0, 100))
    assertThat(allAfter.content.map { it.name }).doesNotContain("to-delete")
  }
}
