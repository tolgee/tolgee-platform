package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder

class ProjectTranslationsStatsTestData {
  var organizationBuilder: OrganizationBuilder
  var admin: UserAccountBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      admin = addUserAccount { username = "admin@admin.com" }

      organizationBuilder = admin.defaultOrganizationBuilder

      addProject { name = "Project" }.build {
        val en = addEnglish()

        addKey { name = "test_key" }.build {
          addTranslation {
            text = "${en.self.name} text"
            language = en.self
          }
        }
      }
    }
}
