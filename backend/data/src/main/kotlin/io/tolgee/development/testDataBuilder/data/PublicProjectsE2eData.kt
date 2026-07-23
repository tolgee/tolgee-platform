package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

class PublicProjectsE2eData(
  count: Int = 6,
  includeForeignOrgProject: Boolean = true,
) : BaseTestData("publicProjectsUser", "Private project") {
  var outsiderProject: Project? = null
  val contributingUser: UserAccount = userAccountBuilder.self

  init {
    root.apply {
      val communityUserBuilder =
        addUserAccount {
          username = "communityUser"
          name = "Community User"
        }

      if (includeForeignOrgProject) {
        outsiderProject =
          addProject(organizationOwner = communityUserBuilder.defaultOrganizationBuilder.self) {
            name = "Community Outsider"
            public = true
          }.build {
            addBaseLanguage()
          }.self
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
