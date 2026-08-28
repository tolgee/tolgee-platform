package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType

class PublicProjectsE2eData(
  count: Int = 6,
  withCommunityPersonas: Boolean = true,
  withForeignOrgProject: Boolean = true,
) : BaseTestData("publicProjectsUser", "Private project") {
  init {
    root.apply {
      val communityUserBuilder =
        addUserAccount {
          username = "communityUser"
          name = "Community User"
        }

      if (withCommunityPersonas) {
        addCommunityPersonas(communityUserBuilder)
      }

      if (withForeignOrgProject) {
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

  private fun TestDataBuilder.addCommunityPersonas(communityUserBuilder: UserAccountBuilder) {
    addUserAccountWithoutOrganization {
      username = "orgLessCommunityUser"
      name = "Org Less Community User"
    }

    val membersOnlyBuilder =
      addUserAccountWithoutOrganization {
        username = "membersOnlyUser"
        name = "Members Only User"
      }

    val membersOnlyOrganization =
      addOrganization {
        name = "Members Only Outfit"
      }.build {
        addRole {
          user = membersOnlyBuilder.self
          type = OrganizationRoleType.OWNER
        }
      }

    addProject(organizationOwner = membersOnlyOrganization.self) {
      name = "Members only private project"
      public = false
    }.build {
      addBaseLanguage()
    }

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
