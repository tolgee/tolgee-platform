package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.model.UserAccount
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class RefreshPreferredOrganizationTest : AbstractSpringTest() {
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
  fun `keeps a community-viewable preference after leaving the organization`() {
    setPreferred(testData.otherOrgMember, testData.otherOrg.id)
    executeInNewTransaction {
      organizationRoleService.removeUser(testData.otherOrgMember.id, testData.otherOrg.id)
    }
    assertThat(preferredOrganizationId(testData.otherOrgMember)).isEqualTo(testData.otherOrg.id)
  }

  @Test
  fun `evicts the preference after leaving an organization without public projects`() {
    setPreferred(testData.noPublicOrgMember, testData.noPublicOrg.id)
    executeInNewTransaction {
      organizationRoleService.removeUser(testData.noPublicOrgMember.id, testData.noPublicOrg.id)
    }
    assertThat(preferredOrganizationId(testData.noPublicOrgMember)).isNotEqualTo(testData.noPublicOrg.id)
  }

  @Test
  fun `keeps a community-viewable preference after direct permission removal`() {
    setPreferred(testData.directPermissionUser, testData.otherOrg.id)
    executeInNewTransaction {
      val permission =
        permissionService
          .getProjectPermissionData(
            testData.otherOrgPublicProject.id,
            testData.directPermissionUser.id,
          ).directPermissions
      permissionService.delete(permission!!.id)
    }
    assertThat(preferredOrganizationId(testData.directPermissionUser)).isEqualTo(testData.otherOrg.id)
  }

  @Test
  fun `soft-deleting the organization evicts the preference`() {
    setPreferred(testData.nonMember, testData.otherOrg.id)
    executeInNewTransaction {
      organizationService.delete(organizationService.get(testData.otherOrg.id))
    }
    assertThat(preferredOrganizationId(testData.nonMember)).isNotEqualTo(testData.otherOrg.id)
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

  private fun preferredOrganizationId(user: UserAccount): Long? {
    return executeInNewTransaction {
      userPreferencesService.find(user.id)?.preferredOrganization?.id
    }
  }
}
