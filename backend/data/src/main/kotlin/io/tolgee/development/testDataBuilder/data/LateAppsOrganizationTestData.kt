package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Organization

/**
 * A second, independent organization with a project, saved after the app under test is already
 * configured — so it stands for an organization that did not exist at configuration time.
 */
class LateAppsOrganizationTestData : BaseTestData("apps-test-late-owner@test.com", "late_apps_project") {
  val organization: Organization
    get() = userAccountBuilder.defaultOrganizationBuilder.self
}
