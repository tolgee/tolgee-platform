package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder

/**
 * The inherited BaseTestData project ("Private project") stays non-public so the list endpoint's
 * exclusion of private projects can be asserted.
 */
class PublicProjectsE2eData(
  count: Int = 6,
  includeForeignOrgProject: Boolean = true,
) : BaseTestData("publicProjectsUser", "Private project") {
  init {
    root.apply {
      val communityUserBuilder =
        addUserAccount {
          username = "communityUser"
          name = "Community User"
        }

      // a dropped organizationId filter in the org-scoped listing would surface this foreign project
      if (includeForeignOrgProject) {
        addProject(organizationOwner = communityUserBuilder.defaultOrganizationBuilder.self) {
          name = "Community Outsider"
          public = true
        }.build {
          addBaseLanguage()
        }
      }

      listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta").take(count).forEach { suffix ->
        addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
          name = "Community $suffix"
          public = true
        }.build {
          addBaseLanguage()
        }
      }
    }
  }

  private fun ProjectBuilder.addBaseLanguage() {
    addLanguage {
      name = "English"
      tag = "en"
      originalName = "English"
      this@addBaseLanguage.self.baseLanguage = this
    }
  }
}
