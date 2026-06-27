package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder

class PublicProjectsStatsTestData {
  var organizationBuilder: OrganizationBuilder
  var admin: UserAccountBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      admin = addUserAccount { username = "public-stats-admin@admin.com" }

      organizationBuilder = admin.defaultOrganizationBuilder
      organizationBuilder.self.name = "StatsOrg"

      addProject {
        name = "Alpha Public"
        public = true
      }.build {
        addEnglish()
      }

      addProject {
        name = "Beta Private"
      }.build {
        addEnglish()
      }
    }
}
