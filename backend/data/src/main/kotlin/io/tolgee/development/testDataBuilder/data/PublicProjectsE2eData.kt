package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Project

/**
 * E2E fixture for the /public-projects page. The BaseTestData project ("Private project") stays
 * non-public so the spec can assert it is absent from the anonymous listing; two public projects in
 * the same org are discoverable.
 */
class PublicProjectsE2eData : BaseTestData("publicProjectsUser", "Private project") {
  lateinit var communityAlpha: Project
  lateinit var communityBeta: Project

  init {
    root.apply {
      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Community Alpha"
        public = true
      }.build {
        communityAlpha = self
        addBaseLanguage()
      }

      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Community Beta"
        public = true
      }.build {
        communityBeta = self
        addBaseLanguage()
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
