package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.DisableManagedUserTestData
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.node
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
    assertMemberFlags("managed@acting.org", managed = true, disabled = true)
  }

  @Test
  fun `re-enables a disabled managed user`() {
    disable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    assertMemberFlags("managed@acting.org", managed = true, disabled = false)
  }

  @Test
  fun `disabled managed user stays in the member listing alongside active members`() {
    assertTotalElements(SEEDED_MEMBER_COUNT)
    disable(testData.managedMember.id).andIsOk
    assertTotalElements(SEEDED_MEMBER_COUNT)
    assertMemberFlags("managed@acting.org", managed = true, disabled = true)
  }

  @Test
  fun `non-managed disabled member is hidden from the org listing`() {
    performAuthGet("/v2/organizations/${testData.organization.id}/users?search=disabled@acting.org")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @Test
  fun `project-only member surfaces managed=false and disabled=false`() {
    assertMemberFlags("projectonly@acting.org", managed = false, disabled = false)
  }

  @Test
  fun `listing collapses fan-out for a member with multiple project permissions`() {
    assertSearchSize("multiproject@acting.org", 1)
    disable(testData.managedMember.id).andIsOk
    assertSearchSize("multiproject@acting.org", 1)
    assertTotalElements(SEEDED_MEMBER_COUNT)
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
  fun `self-enable is rejected by the scope guard`() {
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
    assertMemberFlags("managed@acting.org", managed = true, disabled = true)
  }

  @Test
  fun `enabling an already-enabled managed user is an idempotent no-op`() {
    disable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    enable(testData.managedMember.id).andIsOk
    assertMemberFlags("managed@acting.org", managed = true, disabled = false)
  }

  @Test
  fun `org-disable rejects the managed user's pre-existing JWT and PAT`() {
    val jwt = jwtService.emitToken(testData.managedMember.id)
    val pat = patService.create(CreatePatDto("kill-switch"), testData.managedMember)

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
  fun `org owner can re-enable a user a platform admin disabled (Q3 accepted authority boundary)`() {
    userAccountService.disable(testData.managedMember.id)
    enable(testData.managedMember.id).andIsOk
    assertMemberFlags("managed@acting.org", managed = true, disabled = false)
  }

  @Test
  fun `a non-owner cannot disable a managed user`() {
    userAccount = testData.nonManagedMember
    disable(testData.managedMember.id).andIsForbidden
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
  fun `removing a managed user is still rejected (the DELETE revert)`() {
    performAuthDelete("/v2/organizations/${testData.organization.id}/users/${testData.managedMember.id}", null)
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage(Message.USER_IS_MANAGED_BY_ORGANIZATION.code)
  }

  @Test
  fun `disabled managed user is hidden from project listing but shown in org listing`() {
    disable(testData.managedMember.id).andIsOk
    performAuthGet("/v2/projects/${testData.project.id}/users?search=managed@acting.org")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
    assertMemberFlags("managed@acting.org", managed = true, disabled = true)
  }

  private fun disable(userId: Long) =
    performAuthPut("/v2/organizations/${testData.organization.id}/users/$userId/disable", null)

  private fun enable(userId: Long) =
    performAuthPut("/v2/organizations/${testData.organization.id}/users/$userId/enable", null)

  private fun assertTotalElements(expected: Int) {
    performAuthGet("/v2/organizations/${testData.organization.id}/users?size=100")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(expected)
      }
  }

  private fun assertSearchSize(
    search: String,
    expected: Int,
  ) {
    performAuthGet("/v2/organizations/${testData.organization.id}/users?search=$search")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.usersInOrganization").isArray.hasSize(expected)
      }
  }

  private fun assertMemberFlags(
    search: String,
    managed: Boolean,
    disabled: Boolean,
  ) {
    performAuthGet("/v2/organizations/${testData.organization.id}/users?search=$search")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.usersInOrganization") {
          isArray.hasSize(1)
          node("[0].managed").isEqualTo(managed)
          node("[0].disabled").isEqualTo(disabled)
        }
      }
  }

  companion object {
    private const val SEEDED_MEMBER_COUNT = 5
  }
}
