package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

class AppsTestData {
  lateinit var user: UserAccount

  /** A user with no membership in [organization] / [project] — used to assert acting-as bounds. */
  lateinit var outsider: UserAccount
  lateinit var organization: Organization
  lateinit var project: Project
  lateinit var userAccountBuilder: UserAccountBuilder
  lateinit var projectBuilder: ProjectBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccountBuilder =
        addUserAccount {
          username = "admin"
        }
      user = userAccountBuilder.self
      organization = userAccountBuilder.defaultOrganizationBuilder.self

      outsider = addUserAccount { username = "outsider" }.self

      projectBuilder =
        addProject {
          name = "test_project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }
      project = projectBuilder.self
    }
}
