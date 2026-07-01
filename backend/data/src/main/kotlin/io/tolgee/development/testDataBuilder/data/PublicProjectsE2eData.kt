package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder

/**
 * E2E fixture for the /public-projects page: seeds `count` public projects plus the non-public
 * BaseTestData project ("Private project") used for the exclusion assertion.
 */
class PublicProjectsE2eData(
  count: Int = 6,
) : BaseTestData("publicProjectsUser", "Private project") {
  init {
    root.apply {
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
