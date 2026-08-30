package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.DisableManagedUserTestData
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.UserDisabledBy
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import net.javacrumbs.jsonunit.core.internal.Node.JsonMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerManagedUsersTest : BaseOrganizationControllerTest() {
  lateinit var testData: DisableManagedUserTestData

  @BeforeEach
  fun prepareManagedUsersTestData() {
    testData = DisableManagedUserTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.owner
  }

  @AfterEach
  fun cleanManagedUsersTestData() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `disables a managed user`() {
    disable(testData.managedMember.id).andIsOk
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = true)
  }

  @Test
  fun `re-enables a disabled managed user`() {
    disable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = false)
  }

  @Test
  fun `disabled managed user stays in the member listing alongside active members`() {
    assertListingShowsExactly(visibleMembers)
    disable(testData.managedMember.id).andIsOk
    assertListingShowsExactly(visibleMembers)
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = true)
  }

  @Test
  fun `non-managed disabled member is hidden from the org listing`() {
    assertNotListed(testData.disabledNonManagedMember.username!!)
  }

  @Test
  fun `managed member whose disable origin is unknown is hidden from the org listing`() {
    assertNotListed(testData.nullOriginDisabledManagedMember.username!!)
  }

  @Test
  fun `a member disabled by the organization that manages them elsewhere stays hidden here`() {
    assertNotListed(testData.disabledByOtherOrgPlainMember.username!!)
  }

  @Test
  fun `project-only member surfaces managed=false and disabled=false`() {
    assertMemberFlags(testData.projectOnlyMember.username!!, managed = false, disabled = false)
  }

  @Test
  fun `listing collapses fan-out for a member with multiple project permissions`() {
    assertListedExactlyOnce(testData.multiProjectMember.username!!)
    disable(testData.managedMember.id).andIsOk
    assertListedExactlyOnce(testData.multiProjectMember.username!!)
    assertListingShowsExactly(visibleMembers)
  }

  @Test
  fun `a platform admin acts for the organization through the org endpoints`() {
    userAccount = testData.outsidePlatformAdmin
    disable(testData.managedMember.id).andIsOk
    assertDisabledBy(testData.managedMember.id, UserDisabledBy.ORGANIZATION)
    enable(testData.managedMember.id).andIsOk
    assertDisabledBy(testData.managedMember.id, null)
  }

  @Test
  fun `the role of a disabled managed member can still be changed`() {
    disable(testData.managedMember.id).andIsOk
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/users/${testData.managedMember.id}/set-role",
      SetOrganizationRoleDto(OrganizationRoleType.OWNER),
    ).andIsOk
    assertThat(organizationRoleService.findType(testData.managedMember.id, testData.organization.id))
      .isEqualTo(OrganizationRoleType.OWNER)
  }

  @Test
  fun `the role of a member an admin disabled cannot be changed`() {
    setRole(testData.adminDisabledManagedMember.id).andIsNotFound
  }

  @Test
  fun `the role of a member whose disable origin is unknown cannot be changed`() {
    setRole(testData.nullOriginDisabledManagedMember.id).andIsNotFound
  }

  @Test
  fun `the role of a member another organization disabled cannot be changed`() {
    setRole(testData.disabledByOtherOrgPlainMember.id).andIsNotFound
  }

  @Test
  fun `cannot disable a platform admin managed by the organization`() {
    disable(testData.managedPlatformAdmin.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.CANNOT_MANAGE_PLATFORM_STAFF_ACCOUNT.code)
    assertMemberFlags(testData.managedPlatformAdmin.username!!, managed = true, disabled = false)
  }

  @Test
  fun `cannot disable a platform supporter managed by the organization`() {
    disable(testData.managedPlatformSupporter.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.CANNOT_MANAGE_PLATFORM_STAFF_ACCOUNT.code)
    assertMemberFlags(testData.managedPlatformSupporter.username!!, managed = true, disabled = false)
  }

  @Test
  fun `a disabled staff account attributed to the organization is not listed`() {
    assertNotListed(testData.orgDisabledManagedPlatformAdmin.username!!)
  }

  @Test
  fun `cannot enable a platform admin whose disable was attributed to the organization`() {
    enable(testData.orgDisabledManagedPlatformAdmin.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.CANNOT_MANAGE_PLATFORM_STAFF_ACCOUNT.code)
    assertThat(userAccountService.findActiveOrDisabled(testData.orgDisabledManagedPlatformAdmin.id)!!.disabledAt)
      .isNotNull()
  }

  @Test
  fun `enabling an already-enabled platform admin managed by the organization stays a no-op`() {
    enable(testData.managedPlatformAdmin.id).andIsOk
  }

  @Test
  fun `cannot disable a non-managed member`() {
    disable(testData.nonManagedMember.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_NOT_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `cannot enable a non-managed member`() {
    enable(testData.nonManagedMember.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_NOT_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `cannot disable a user managed by another organization`() {
    disable(testData.managedByOtherOrg.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_NOT_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `cannot disable own account`() {
    disable(testData.owner.id)
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_DISABLE_YOUR_OWN_ACCOUNT)
  }

  @Test
  fun `an owner cannot enable themselves, they are not a managed member`() {
    enable(testData.owner.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_NOT_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `disabling an already-disabled managed user is an idempotent no-op`() {
    disable(testData.managedMember.id).andIsOk
    disable(testData.managedMember.id).andIsOk
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = true)
  }

  @Test
  fun `enabling an already-enabled managed user is an idempotent no-op`() {
    disable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = false)
  }

  @Test
  fun `org-disable rejects the managed user's pre-existing JWT and PAT`() {
    val jwt = jwtService.emitToken(testData.managedMember.id)
    val pat = testData.managedMemberPat

    disable(testData.managedMember.id).andIsOk

    performGet(
      "/v2/user",
      HttpHeaders().apply { add("Authorization", "Bearer $jwt") },
    ).andIsUnauthorized
    performGet(
      "/v2/user",
      HttpHeaders().apply { add("X-API-Key", "tgpat_${pat.token}") },
    ).andIsUnauthorized
  }

  @Test
  fun `re-enabling a managed user restores their access`() {
    disable(testData.managedMember.id).andIsOk
    performGet(
      "/v2/user",
      HttpHeaders().apply { add("Authorization", "Bearer ${jwtService.emitToken(testData.managedMember.id)}") },
    ).andIsUnauthorized

    enable(testData.managedMember.id).andIsOk
    performGet(
      "/v2/user",
      HttpHeaders().apply { add("Authorization", "Bearer ${jwtService.emitToken(testData.managedMember.id)}") },
    ).andIsOk
  }

  @Test
  fun `cannot enable a user managed by another organization`() {
    enable(testData.managedByOtherOrg.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_NOT_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `org owner cannot re-enable a user a platform admin disabled`() {
    userAccountService.disable(testData.managedMember.id, UserDisabledBy.ADMIN)
    enable(testData.managedMember.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_DISABLED_BY_ADMIN.code)
    assertDisabledBy(testData.managedMember.id, UserDisabledBy.ADMIN)
  }

  @Test
  fun `an org disable is rejected, not silently dropped, on an admin-disabled account`() {
    userAccountService.disable(testData.managedMember.id, UserDisabledBy.ADMIN)
    disable(testData.managedMember.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_DISABLED_BY_ADMIN.code)
    assertDisabledBy(testData.managedMember.id, UserDisabledBy.ADMIN)
  }

  @Test
  fun `an admin disable takes over an org disable`() {
    disable(testData.managedMember.id).andIsOk
    userAccountService.disable(testData.managedMember.id, UserDisabledBy.ADMIN)
    assertDisabledBy(testData.managedMember.id, UserDisabledBy.ADMIN)
    enable(testData.managedMember.id)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_DISABLED_BY_ADMIN.code)
  }

  @Test
  fun `an org disable records the organization as the origin`() {
    disable(testData.managedMember.id).andIsOk
    assertDisabledBy(testData.managedMember.id, UserDisabledBy.ORGANIZATION)
  }

  @Test
  fun `enabling clears the disable origin`() {
    disable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    assertDisabledBy(testData.managedMember.id, null)
  }

  @Test
  fun `a platform admin can enable a user the organization disabled`() {
    disable(testData.managedMember.id).andIsOk
    userAccountService.enable(testData.managedMember.id, UserDisabledBy.ADMIN)
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = false)
    assertDisabledBy(testData.managedMember.id, null)
  }

  @Test
  fun `admin-disabled managed user is hidden from the org listing`() {
    assertNotListed(testData.adminDisabledManagedMember.username!!)
  }

  @Test
  fun `org-disabled managed user stays visible in the org listing`() {
    assertMemberFlags(testData.orgDisabledManagedMember.username!!, managed = true, disabled = true)
  }

  @Test
  fun `a non-owner cannot disable a managed user`() {
    userAccount = testData.nonManagedMember
    disable(testData.managedMember.id).andIsForbidden
  }

  @Test
  fun `a non-owner cannot enable a managed user`() {
    disable(testData.managedMember.id).andIsOk
    userAccount = testData.nonManagedMember
    enable(testData.managedMember.id).andIsForbidden
  }

  @Test
  fun `a managed user cannot leave their managing organization`() {
    userAccount = testData.managedMember
    performAuthPut("/v2/organizations/${testData.organization.id}/leave", null)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `a member managed by another organization can still be removed from this one`() {
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/users/${testData.managedByOtherOrg.id}",
      null,
    ).andIsOk
  }

  @Test
  fun `a member an admin disabled cannot be removed`() {
    performAuthDelete(
      "/v2/organizations/${testData.organization.id}/users/${testData.disabledNonManagedMember.id}",
      null,
    ).andIsNotFound
  }

  @Test
  fun `removing a managed user is rejected`() {
    performAuthDelete("/v2/organizations/${testData.organization.id}/users/${testData.managedMember.id}", null)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `disabled managed user is hidden from project listing but shown in org listing`() {
    disable(testData.managedMember.id).andIsOk
    performAuthGet("/v2/projects/${testData.project.id}/users?search=${testData.managedMember.username}")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
    assertMemberFlags(testData.managedMember.username!!, managed = true, disabled = true)
  }

  private fun disable(userId: Long) =
    performAuthPut("/v2/organizations/${testData.organization.id}/users/$userId/disable", null)

  private fun enable(userId: Long) =
    performAuthPut("/v2/organizations/${testData.organization.id}/users/$userId/enable", null)

  private fun assertListingShowsExactly(expected: List<UserAccount>) {
    val expectedUsernames = expected.map { it.username }.toSet()
    performAuthGet("/v2/organizations/${testData.organization.id}/users?size=100")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.usersInOrganization") {
          isArray.hasSize(expectedUsernames.size)
          expectedUsernames.forEach { username ->
            isArray.anySatisfy { (it as JsonMap)["username"].assert.isEqualTo(username) }
          }
        }
      }
  }

  private fun setRole(userId: Long) =
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/users/$userId/set-role",
      SetOrganizationRoleDto(OrganizationRoleType.OWNER),
    )

  private fun search(username: String) =
    performAuthGet("/v2/organizations/${testData.organization.id}/users?search=$username").andIsOk

  private fun assertListedExactlyOnce(username: String) {
    search(username).andAssertThatJson {
      node("_embedded.usersInOrganization").isArray.hasSize(1)
    }
  }

  private fun assertNotListed(username: String) {
    search(username).andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  private fun assertMemberFlags(
    username: String,
    managed: Boolean,
    disabled: Boolean,
  ) {
    search(username)
      .andAssertThatJson {
        node("_embedded.usersInOrganization") {
          isArray.hasSize(1)
          node("[0].managed").isEqualTo(managed)
          node("[0].disabled").isEqualTo(disabled)
        }
      }
  }

  private fun assertDisabledBy(
    userId: Long,
    expected: UserDisabledBy?,
  ) {
    assertThat(userAccountService.findActiveOrDisabled(userId)!!.disabledBy).isEqualTo(expected)
  }

  private val visibleMembers
    get() =
      listOf(
        testData.owner,
        testData.managedMember,
        testData.nonManagedMember,
        testData.orgDisabledManagedMember,
        testData.managedPlatformAdmin,
        testData.managedPlatformSupporter,
        testData.managedByOtherOrg,
        testData.projectOnlyMember,
        testData.multiProjectMember,
      )
}
