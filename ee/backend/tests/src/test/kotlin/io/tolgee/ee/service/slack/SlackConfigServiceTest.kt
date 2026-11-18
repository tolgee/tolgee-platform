package io.tolgee.ee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SlackConfigServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var slackConfigManageService: SlackConfigManageService

  @Autowired
  lateinit var slackConfigReadService: SlackConfigReadService

  lateinit var testData: SlackTestData

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    testData.addLanguageOnlySlackConfigWithChannelEvents()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `deletes configs`() {
    slackConfigManageService.delete(testData.projectBuilder.self.id, testData.slackConfig.channelId, "")
    Assertions.assertThat(slackConfigReadService.find(testData.projectBuilder.self.id, "testChannel")).isNull()
  }

  @Test
  fun `creates new config`() {
    val slackConfigDto =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = "testChannel2",
        userAccount = testData.user,
        events = mutableSetOf(SlackEventType.ALL),
        isGlobal = true,
        slackTeamId = "slackTeamId",
      )
    slackConfigManageService.createOrUpdate(slackConfigDto)
    Assertions.assertThat(slackConfigReadService.find(testData.projectBuilder.self.id, "testChannel2")).isNotNull()
  }

  @Test
  fun `sets all events on the channel when switching a language subscription to global`() {
    slackConfigManageService.createOrUpdate(
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = testData.languageOnlySlackConfig.channelId,
        userAccount = testData.user,
        isGlobal = true,
        slackTeamId = "slackTeamId",
      ),
    )

    executeInNewTransaction {
      val config = slackConfigReadService.find(testData.projectBuilder.self.id, "languageOnlyChannel")!!
      config.isGlobalSubscription.assert.isTrue()
      config.events.assert.containsExactly(SlackEventType.ALL)
    }
  }

  @Test
  fun `creates global config with language preference when both are requested`() {
    slackConfigManageService.createOrUpdate(
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = "testChannel2",
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(SlackEventType.NEW_KEY),
        isGlobal = true,
        slackTeamId = "slackTeamId",
      ),
    )

    executeInNewTransaction {
      val config = slackConfigReadService.find(testData.projectBuilder.self.id, "testChannel2")!!
      config.isGlobalSubscription.assert.isTrue()
      config.events.assert.containsExactly(SlackEventType.ALL)
      config.preferences.map { it.languageTag to it.events }.assert.containsExactly(
        "fr" to mutableSetOf(SlackEventType.NEW_KEY),
      )
    }
  }

  @Test
  fun `transferring a project removes subscriptions of the workspaces of the organization it leaves`() {
    val projectId = testData.projectBuilder.self.id
    val otherOrganization =
      dbPopulator.createOrganizationIfNotExist("slack-transfer-target", userAccount = testData.user)
    executeInNewTransaction {
      entityManager
        .createQuery("update OrganizationSlackWorkspace w set w.organization = :organization where w.id = :id")
        .setParameter("organization", otherOrganization)
        .setParameter("id", testData.slackWorkspace2.id)
        .executeUpdate()
      bindToWorkspace(testData.slackConfig, testData.slackWorkspace)
      bindToWorkspace(testData.languageOnlySlackConfig, testData.slackWorkspace2)
    }

    projectService.transferToOrganization(projectId, otherOrganization.id)

    executeInNewTransaction {
      slackConfigReadService.find(projectId, "testChannel").assert.isNull()
      slackConfigReadService.find(projectId, "languageOnlyChannel").assert.isNotNull()
      entityManager
        .createQuery("select count(a) from Automation a where a.project.id = :projectId")
        .setParameter("projectId", projectId)
        .singleResult
        .assert
        .isEqualTo(0L)
    }
  }

  private fun bindToWorkspace(
    config: SlackConfig,
    workspace: OrganizationSlackWorkspace,
  ) {
    entityManager
      .createQuery("update SlackConfig s set s.organizationSlackWorkspace = :workspace where s.id = :id")
      .setParameter("workspace", workspace)
      .setParameter("id", config.id)
      .executeUpdate()
  }
}
