package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace

class SlackNoUserConnectionTestData {
  var user: UserAccount
  var userAccountBuilder: UserAccountBuilder
  var projectBuilder: ProjectBuilder
  var organization: Organization
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
        }.build buildProject@{
          this@buildProject.self.baseLanguage = this@buildProject.addEnglish().self
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

      projectBuilder.addPermission {
        project = projectBuilder.self
        user = user
        type = ProjectPermissionType.MANAGE
        scopes = arrayOf(Scope.TRANSLATIONS_EDIT)
      }
    }
}
