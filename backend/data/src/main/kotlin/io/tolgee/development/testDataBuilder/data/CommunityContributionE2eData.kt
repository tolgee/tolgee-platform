package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Project

class CommunityContributionE2eData : BaseTestData("communityContributionOwner", "Owner private project") {
  lateinit var publicProject: Project

  init {
    root.apply {
      publicProject =
        addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
          name = "Contributed public project"
          public = true
        }.build {
          addBaseLanguage()
        }.self
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
