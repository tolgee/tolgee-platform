package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

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

      addUserAccountWithoutOrganization {
        username = "orgLessCommunityUser"
        name = "Org Less Community User"
      }

      addUserAccountWithoutOrganization {
        username = "supporterCommunityUser"
        name = "Supporter Community User"
        role = UserAccount.Role.SUPPORTER
      }

      val adminMemberBuilder =
        addUserAccountWithoutOrganization {
          username = "adminMemberUser"
          name = "Admin Member User"
          role = UserAccount.Role.ADMIN
        }

      userAccountBuilder.defaultOrganizationBuilder.build {
        addRole {
          user = adminMemberBuilder.self
          type = OrganizationRoleType.MEMBER
        }
      }

      if (includeForeignOrgProject) {
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
