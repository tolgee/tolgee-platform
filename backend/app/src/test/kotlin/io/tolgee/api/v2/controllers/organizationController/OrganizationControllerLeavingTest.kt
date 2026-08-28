package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.development.testDataBuilder.data.OwnerlessOrganizationTestData
import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.PageRequest

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerLeavingTest : BaseOrganizationControllerTest() {
  @Test
  fun testLeaveOrganization() {
    val testOrg =
      executeInNewTransaction { this.organizationService.createWithoutAuthorization(dummyDto, userAccount!!) }
    organizationRoleService.grantOwnerRoleToUser(dbPopulator.createUserIfNotExists("secondOwner"), testOrg)
    assertThat(getPermittedOrgs().find { testOrg.id == it.id }).isNotNull
    performAuthPut("/v2/organizations/${testOrg.id}/leave", null).andIsOk
    assertThat(getPermittedOrgs().find { testOrg.id == it.id }).isNull()
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

  private var ownerlessTestData: OwnerlessOrganizationTestData? = null

  @AfterEach
  fun cleanOwnerlessTestData() {
    ownerlessTestData?.let { testDataService.cleanTestData(it.root) }
    ownerlessTestData = null
  }

  private fun saveOwnerless(): OwnerlessOrganizationTestData {
    val data = OwnerlessOrganizationTestData()
    ownerlessTestData = data
    testDataService.saveTestData(data.root)
    return data
  }

  @Test
  fun `a member of an ownerless organization can leave it`() {
    val ownerless = saveOwnerless()

    userAccount = ownerless.member
    performAuthPut("/v2/organizations/${ownerless.withMember.id}/leave", null).andIsOk
    assertThat(
      organizationRepository.findAllPermitted(ownerless.member.id, PageRequest.of(0, 20)).content.find {
        ownerless.withMember.id == it.id
      },
    ).isNull()
  }

  @Test
  fun `leaving an organization with no owners reports not-a-member, not the owner count`() {
    val ownerless = saveOwnerless()

    performAuthPut("/v2/organizations/${ownerless.withPublicProject.id}/leave", null)
      .andIsNotFound
      .andAssertThatJson {
        node("code").isEqualTo("user_is_not_member_of_organization")
      }
  }

  @Test
  fun testLeaveOrganizationNoOtherOwner() {
    val organization =
      executeInNewTransaction { this.organizationService.createWithoutAuthorization(dummyDto, userAccount!!) }
    organizationRepository.findAllPermitted(userAccount!!.id, PageRequest.of(0, 20)).content.let {
      assertThat(it).isNotEmpty
    }
    performAuthPut("/v2/organizations/${organization.id}/leave", null)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("organization_has_no_other_owner")
  }
}
