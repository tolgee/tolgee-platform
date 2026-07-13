package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PreferredOrganizationCommunityTest : AuthorizedControllerTest() {
  lateinit var testData: PublicProjectsControllerTestData

  @BeforeEach
  fun setup() {
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
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

  private fun assertStoredPreference(
    userId: Long,
    organizationId: Long,
  ) {
    executeInNewTransaction {
      assertThat(userPreferencesService.find(userId)!!.preferredOrganization!!.id).isEqualTo(organizationId)
    }
  }
}
