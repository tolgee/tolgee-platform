package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackUserConnection

class SlackTestData() {
  var user: UserAccount
  var slackConfig: SlackConfig
  var userAccountBuilder: UserAccountBuilder
  var projectBuilder: ProjectBuilder
  var organization: Organization

  lateinit var slackWorkspace: OrganizationSlackWorkspace
  lateinit var slackUserConnection: SlackUserConnection

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccountBuilder =
        addUserAccount {
          username = "admin"
        }

      userAccountBuilder.addSlackUserConnection {
        userAccount = userAccountBuilder.self
        slackUserId = "slackUserId"
        slackUserConnection = this
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
        organization = userAccountBuilder.defaultOrganizationBuilder.self
        slackWorkspace = this
      }
      organization = userAccountBuilder.defaultOrganizationBuilder.self

      user = userAccountBuilder.self
      slackConfig =
        projectBuilder.addSlackConfig {
          this.channelId = "channel"
          this.project = projectBuilder.self
          this.userAccount = userAccountBuilder.self
        }.self
    }
}
