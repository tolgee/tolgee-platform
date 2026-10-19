package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.Feature
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount

class OrganizationFeatureGrantsTestData : BaseTestData("grantsOrgOwner", "feature_grants_project") {
  lateinit var admin: UserAccount

  lateinit var publicProjectOnlyViewer: UserAccount
  lateinit var otherOrganization: Organization

  val organization: Organization get() = userAccountBuilder.defaultOrganizationBuilder.self

  init {
    projectBuilder.self.public = true
    root.apply {
      addUserAccount {
        username = "grantsAdmin"
        role = UserAccount.Role.ADMIN
        admin = this
      }

      val otherUserBuilder =
        addUserAccount {
          username = "grantsOtherOrgOwner"
        }
      otherOrganization = otherUserBuilder.defaultOrganizationBuilder.self

      addUserAccount {
        username = "grantsPublicProjectOnlyViewer"
        publicProjectOnlyViewer = this
      }
    }
  }

  fun addGrant(feature: Feature) {
    userAccountBuilder.defaultOrganizationBuilder.addFeatureGrant {
      this.feature = feature
    }
  }
}
