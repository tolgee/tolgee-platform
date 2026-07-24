package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

/**
 * Dedicated e2e fixture for the Members-page Community tab. Deliberately NOT ContributorsTestData:
 * that fixture carries soft-deleted and disabled users (it tests their exclusion), which
 * cleanTestData's findActive() skips on cleanup, so they survive and collide on the next generate
 * — fatal for the e2e generate/clean lifecycle. Every user here is active and clean-removable.
 */
class MembersCommunityE2eData {
  lateinit var admin: UserAccount
  lateinit var contributor: UserAccount
  lateinit var contributor2: UserAccount
  lateinit var publicProject: Project
  lateinit var publicEmptyProject: Project
  lateinit var privateProject: Project

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val adminBuilder = addUserAccount { username = "membersCommunityAdmin" }
      admin = adminBuilder.self

      contributor =
        addUserAccount {
          username = "cora.contributor@example.com"
          name = "Cora Contributor"
        }.self

      contributor2 =
        addUserAccount {
          username = "cody.contributor@example.com"
          name = "Cody Contributor"
        }.self

      publicProject =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Members Community Public"
          public = true
        }.self

      publicEmptyProject =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Members Community Public Empty"
          public = true
        }.self

      privateProject =
        addProject(organizationOwner = adminBuilder.defaultOrganizationBuilder.self) {
          name = "Members Community Private"
          public = false
        }.self
    }
}
