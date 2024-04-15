package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SlackConfig

open class SlackTestData() {
  var user: UserAccount
  var slackConfig: SlackConfig
  var userAccountBuilder: UserAccountBuilder
  var projectBuilder: ProjectBuilder

  lateinit var slackWorkspace: OrganizationSlackWorkspace

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccountBuilder =
        addUserAccount {
          username = "admin"
        }

      projectBuilder =
        addProject {
          name = "projectName"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }

      userAccountBuilder.defaultOrganizationBuilder.addSlackWorkspace {
        author = userAccountBuilder.self
        slackTeamId = "slackTeamId"
        slackTeamName = "slackTeamName"
        accessToken = "accessToken"
        slackWorkspace = this
      }

      user = userAccountBuilder.self
      slackConfig =
        projectBuilder.addSlackConfig {
          this.channelId = "channel"
          this.project = projectBuilder.self
          this.userAccount = userAccountBuilder.self
        }.self
    }
}
