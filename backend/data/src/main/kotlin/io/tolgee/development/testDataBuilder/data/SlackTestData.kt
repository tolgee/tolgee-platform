package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace

open class SlackTestData() {
  var user: UserAccount
  var userAccountBuilder: UserAccountBuilder
  lateinit var slackWorkspace: OrganizationSlackWorkspace

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccountBuilder =
        addUserAccount {
          username = "admin"
        }

      userAccountBuilder.defaultOrganizationBuilder.addSlackWorkspace {
        author = userAccountBuilder.self
        slackTeamId = "slackTeamId"
        slackTeamName = "slackTeamName"
        accessToken = "accessToken"
        slackWorkspace = this
      }

      user = userAccountBuilder.self
    }
}
