package io.tolgee.ee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SlackConfigProjectTransferTest : AbstractSpringTest() {
  @Autowired
  lateinit var slackConfigReadService: SlackConfigReadService

  lateinit var testData: SlackTestData

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    testData.addLanguageOnlySlackConfigWithChannelEvents()
    testData.bindSlackConfigsToWorkspacesOfTwoOrganizations()
    testData.addSlackConfigWithoutWorkspace()
    testData.addAutomationForLanguageOnlySlackConfig()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `transferring a project removes only subscriptions of the workspaces of the organization it leaves`() {
    projectService.transferToOrganization(projectId, testData.otherOrganization.id)

    executeInNewTransaction {
      slackConfigReadService.find(projectId, testData.slackConfig.channelId).assert.isNull()
      slackConfigReadService.find(projectId, testData.languageOnlySlackConfig.channelId).assert.isNotNull()
      slackConfigReadService.find(projectId, testData.noWorkspaceSlackConfig.channelId).assert.isNotNull()
      automationIds().assert.containsExactly(testData.languageOnlySlackConfigAutomation.id)
    }
  }

  @Test
  fun `transferring a project to the organization it is already in keeps its subscriptions`() {
    projectService.transferToOrganization(projectId, testData.organization.id)

    executeInNewTransaction {
      slackConfigReadService.find(projectId, testData.slackConfig.channelId).assert.isNotNull()
      automationIds().assert.containsExactlyInAnyOrder(
        testData.automation.id,
        testData.languageOnlySlackConfigAutomation.id,
      )
    }
  }

  private val projectId get() = testData.projectBuilder.self.id

  private fun automationIds(): List<*> =
    entityManager
      .createQuery("select a.id from Automation a where a.project.id = :projectId")
      .setParameter("projectId", projectId)
      .resultList
}
