package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class PublicProjectsE2eData(
  count: Int = 6,
  scenario: Scenario = Scenario.FULL,
) : BaseTestData("publicProjectsUser", "Private project") {
  enum class Scenario {
    FULL,

    /** Omits the extra personas and the foreign-organization project. */
    MINIMAL,
  }

  init {
    root.apply {
      val communityUserBuilder =
        addUserAccount {
          username = "communityUser"
          name = "Community User"
        }

      addUserAccountWithoutOrganization {
        username = "orgLessCommunityUser"
        name = "Org Less Community User"
      }

      if (scenario == Scenario.FULL) {
        val supporterBuilder =
          addUserAccountWithoutOrganization {
            username = "supporterCommunityUser"
            name = "Supporter Community User"
            role = UserAccount.Role.SUPPORTER
          }

        val dualOrgBuilder =
          addUserAccount {
            username = "dualOrgCommunityUser"
            name = "Dual Org Member"
          }

        // Without this the server picks the organization this user only belongs to.
        dualOrgBuilder.build {
          setUserPreferences {
            preferredOrganization = dualOrgBuilder.defaultOrganizationBuilder.self
          }
        }

        communityUserBuilder.defaultOrganizationBuilder.build {
          addRole {
            user = supporterBuilder.self
            type = OrganizationRoleType.MEMBER
          }
          addRole {
            user = dualOrgBuilder.self
            type = OrganizationRoleType.MEMBER
          }
        }

        addProject(organizationOwner = communityUserBuilder.defaultOrganizationBuilder.self) {
          name = "Community Outsider"
          public = true
        }.build {
          addBaseLanguage()
          addContribution(author = userAccountBuilder.self)
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
