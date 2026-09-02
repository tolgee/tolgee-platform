package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.UserDisabledBy
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.PageRequest

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerLeavingTest : BaseOrganizationControllerTest() {
  @Test
  fun testLeaveOrganization() {
    val testOrg = executeInNewTransaction { this.organizationService.create(dummyDto, userAccount!!) }
    organizationRoleService.grantOwnerRoleToUser(dbPopulator.createUserIfNotExists("secondOwner"), testOrg)
    getPermittedOrgs().find { testOrg.id == it.id }.assert.isNotNull
    performAuthPut("/v2/organizations/${testOrg.id}/leave", null).andIsOk
    getPermittedOrgs().find { testOrg.id == it.id }.assert.isNull()
  }

  private fun getPermittedOrgs() =
    organizationRepository.findAllPermitted(userAccount!!.id, PageRequest.of(0, 20)).content

  @Test
  fun `leave will reset preferred`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.jirina

    executeInNewTransaction {
      assertThat(userPreferencesService.find(testData.jirina.id)!!.preferredOrganization?.name)
        .isEqualTo(testData.jirinaOrg.name)

      performAuthPut("/v2/organizations/${testData.jirinaOrg.id}/leave", null).andIsOk

      assertThat(userPreferencesService.find(testData.jirina.id)!!.preferredOrganization?.name)
        .isNotEqualTo(testData.jirinaOrg.name)
    }
  }

  @Test
  fun `removes all direct permissions when leaving`() {
    val testData = PermissionsTestData()
    val me = testData.addUserWithPermissions(type = ProjectPermissionType.MANAGE)
    testDataService.saveTestData(testData.root)
    userAccount = me

    permissionService
      .getProjectPermissionData(
        testData.projectBuilder.self.id,
        me.id,
      ).directPermissions.assert.isNotNull

    performAuthPut("/v2/organizations/${testData.organizationBuilder.self.id}/leave", null).andIsOk

    permissionService
      .getProjectPermissionData(
        testData.projectBuilder.self.id,
        me.id,
      ).directPermissions.assert
      .isNull()
  }

  @Test
  fun testLeaveOrganizationNoOtherOwner() {
    val organization = executeInNewTransaction { this.organizationService.create(dummyDto, userAccount!!) }
    organizationRepository.findAllPermitted(userAccount!!.id, PageRequest.of(0, 20)).content.let {
      assertThat(it).isNotEmpty
    }
    performAuthPut("/v2/organizations/${organization.id}/leave", null)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("organization_has_no_other_owner")
  }

  @Test
  fun `cannot leave when the only other owner is disabled`() {
    val organization = executeInNewTransaction { this.organizationService.create(dummyDto, userAccount!!) }
    val secondOwner = dbPopulator.createUserIfNotExists(DISABLED_SECOND_OWNER)
    organizationRoleService.grantOwnerRoleToUser(secondOwner, organization)
    userAccountService.disable(secondOwner.id, UserDisabledBy.ADMIN)

    performAuthPut("/v2/organizations/${organization.id}/leave", null)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("organization_has_no_other_owner")
  }

  @Test
  fun `a member can leave when the only owner is disabled`() {
    val owner = dbPopulator.createUserIfNotExists(DISABLED_SOLE_OWNER)
    val organization = executeInNewTransaction { this.organizationService.create(dummyDto, owner) }
    executeInNewTransaction {
      organizationRoleService.grantMemberRoleToUser(userAccountService.get(userAccount!!.id), organization)
    }
    userAccountService.disable(owner.id, UserDisabledBy.ADMIN)

    performAuthPut("/v2/organizations/${organization.id}/leave", null).andIsOk
    getPermittedOrgs().find { organization.id == it.id }.assert.isNull()
  }

  companion object {
    private const val DISABLED_SECOND_OWNER = "disabledSecondOwner"
    private const val DISABLED_SOLE_OWNER = "disabledSoleOwner"
  }
}
