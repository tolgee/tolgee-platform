package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates

class PreferredOrganizationCommunityTest : AuthorizedControllerTest() {
  lateinit var testData: PublicProjectsControllerTestData

  private var originalUserCanCreateOrganizations by Delegates.notNull<Boolean>()

  @BeforeEach
  fun setup() {
    originalUserCanCreateOrganizations = tolgeeProperties.authentication.userCanCreateOrganizations
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    tolgeeProperties.authentication.userCanCreateOrganizations = originalUserCanCreateOrganizations
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `community user gets the reduced organization model`() {
    setPreferred(testData.nonMember, testData.otherOrg.id)
    userAccount = testData.nonMember
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(true)
      node("basePermissions").isNotNull
      node("activeCloudSubscription").isEqualTo(null)
    }
  }

  @Test
  fun `member gets the full organization model`() {
    setPreferred(testData.otherOrgMember, testData.otherOrg.id)
    userAccount = testData.otherOrgMember
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo("MEMBER")
      node("limitedView").isEqualTo(false)
    }
  }

  @Test
  fun `direct project permission user gets the reduced model with no role but a non-limited view`() {
    setPreferred(testData.directPermissionUser, testData.otherOrg.id)
    userAccount = testData.directPermissionUser
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
      node("activeCloudSubscription").isEqualTo(null)
    }
  }

  @Test
  fun `stale preference heals to a member organization`() {
    setPreferred(testData.user, testData.otherOrg.id)
    unpublishOtherOrgProject()

    userAccount = testData.user
    val ownOrg = testData.userAccountBuilder.defaultOrganizationBuilder.self
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization.id").isEqualTo(ownOrg.id)
      node("preferredOrganization.currentUserRole").isEqualTo("OWNER")
    }
    assertStoredPreference(testData.user.id, ownOrg.id)
  }

  @Test
  fun `stale preference with no other viewable organization heals to a created one`() {
    setPreferred(testData.nonMember, testData.otherOrg.id)
    unpublishOtherOrgProject()
    executeInNewTransaction {
      organizationService.delete(organizationService.get(testData.nonMemberPersonalOrg.id))
    }

    userAccount = testData.nonMember
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization.name").isEqualTo("Non Member")
      node("preferredOrganization.currentUserRole").isEqualTo("OWNER")
    }
    executeInNewTransaction {
      val preferred = userPreferencesService.find(testData.nonMember.id)!!.preferredOrganization!!
      assertThat(preferred.id).isNotEqualTo(testData.otherOrg.id)
      assertThat(preferred.name).isEqualTo("Non Member")
    }
  }

  @Test
  fun `org-less user has no preferred organization until it adopts one with public projects`() {
    disableOrganizationCreation()
    loginAsOrgLessUser()

    performAuthGet("/v2/preferred-organization").andIsForbidden
    performAuthGet("/v2/public/initial-data").andIsOk.andAssertThatJson {
      node("preferredOrganization").isEqualTo(null)
    }

    performAuthPut(
      "/v2/user-preferences/set-preferred-organization/${testData.otherOrg.id}",
      null,
    ).andIsOk

    assertStoredPreference(testData.orgLessCommunityUser.id, testData.otherOrg.id)
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isEqualTo("Vibrant translators")
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(true)
    }
  }

  @Test
  fun `org-less user cannot adopt an organization without public projects`() {
    disableOrganizationCreation()
    loginAsOrgLessUser()

    performAuthPut(
      "/v2/user-preferences/set-preferred-organization/${testData.noPublicOrg.id}",
      null,
    ).andIsForbidden
    performAuthGet("/v2/preferred-organization").andIsForbidden
    assertNoStoredPreference(testData.orgLessCommunityUser.id)
  }

  private fun loginAsOrgLessUser() {
    userAccount = testData.orgLessCommunityUser
  }

  private fun disableOrganizationCreation() {
    tolgeeProperties.authentication.userCanCreateOrganizations = false
  }

  private fun setPreferred(
    user: UserAccount,
    organizationId: Long,
  ) {
    executeInNewTransaction {
      userPreferencesService.setPreferredOrganization(
        organizationService.get(organizationId),
        userAccountService.get(user.id),
      )
    }
  }

  private fun unpublishOtherOrgProject() {
    executeInNewTransaction {
      projectService.get(testData.otherOrgPublicProject.id).public = false
    }
  }

  private fun assertNoStoredPreference(userId: Long) {
    executeInNewTransaction {
      assertThat(userPreferencesService.find(userId)?.preferredOrganization).isNull()
    }
  }

  private fun assertStoredPreference(
    userId: Long,
    organizationId: Long,
  ) {
    executeInNewTransaction {
      assertThat(userPreferencesService.find(userId)!!.preferredOrganization!!.id).isEqualTo(organizationId)
    }
  }
}
