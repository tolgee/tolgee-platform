package io.tolgee.development.testDataBuilder.data

class ProjectsListDashboardTestData : ProjectsTestData() {
  init {
    userAccountBuilder.defaultOrganizationBuilder.self.name = "Dashboard Test Org"
    project2.public = true
  }
}
