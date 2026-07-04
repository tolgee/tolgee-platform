package io.tolgee.api.v2.controllers.organizationController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.dtos.request.userAccount.UserAccountPermissionsFilters
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest

/**
 * The org view floor (`canUserViewStrictOrPublic` = permission/role standing OR a public project)
 * and the below-member surface scoping it enables. Below-member viewers (public-project community
 * users, direct-permission holders) hold no org role — the tests below pin that surface.
 */
class OrganizationFloorAccessTest : AuthorizedControllerTest() {
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
  fun `a public-project floor viewer does not inherit the organization base permission`() {
    userAccount = testData.storedGuest
    performAuthGet("/v2/projects/${testData.otherOrgPrivateProject.id}").andIsNotFound
    performAuthGet("/v2/projects/${testData.otherOrgPublicProject.id}").andIsOk.andAssertThatJson {
      node("computedPermission.origin").isEqualTo("COMMUNITY")
    }
  }

  @Test
  fun `a floor viewer sees only public projects in the organization listing`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations/${testData.otherOrg.slug}/projects-with-stats")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.projects") {
          isArray.hasSize(1)
          node("[0].id").isEqualTo(testData.otherOrgPublicProject.id)
        }
      }
  }

  @Test
  fun `member sees private and public projects in the organization listing`() {
    userAccount = testData.otherOrgMember
    performAuthGet("/v2/organizations/${testData.otherOrg.slug}/projects-with-stats")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.projects").isArray.hasSize(2)
      }
  }

  @Test
  fun `a floor viewer personal project list contains nothing from the organization`() {
    userAccount = testData.storedGuest
    performAuthGet("/v2/projects").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  fun `a floor viewer is denied member endpoints`() {
    userAccount = testData.storedGuest
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/users").andIsForbidden
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/usage").andIsForbidden
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/translation-memories").andIsForbidden
  }

  @Test
  fun `llm providers are floor-visible`() {
    userAccount = testData.storedGuest
    performAuthGet("/v2/organizations/${testData.otherOrg.id}/llm-providers/all-available").andIsOk
  }

  @Test
  fun `a floor viewer reports no role on the org endpoint`() {
    userAccount = testData.storedGuest
    performAuthGet("/v2/organizations/${testData.otherOrg.id}").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
    }
  }

  @Test
  fun `the organizations listing stays membership-scoped - a floor viewer is not in it`() {
    // The floor admits a community viewer to GET an org by id, but the /v2/organizations listing
    // (the org switcher) is role/permission-scoped: a public-project-only viewer is not a member,
    // so the org must not appear there (community users reach it via the community-projects page).
    userAccount = testData.nonMember
    performAuthGet("/v2/organizations?search=Vibrant").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }

    userAccount = testData.otherOrgMember
    performAuthGet("/v2/organizations?search=Vibrant").andIsOk.andAssertThatJson {
      node("_embedded.organizations").isArray.hasSize(1)
    }
  }

  @Test
  fun `a member of a private-only org has standing and reports the MEMBER role`() {
    userAccount = testData.privateOrgMember
    performAuthGet("/v2/organizations/${testData.noPublicOrg.id}").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo("MEMBER")
    }
  }

  @Test
  fun `server admin sees no role on the org endpoint`() {
    userAccount = testData.serverAdmin
    performAuthGet("/v2/organizations/${testData.otherOrg.id}").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
    }
  }

  @Test
  fun `a granular scopes-only permission grants standing but no role`() {
    executeInNewTransaction {
      organizationRoleService
        .canUserViewStrictOrPublic(testData.granularPermissionUser.id, testData.otherOrg.id)
        .assert
        .isTrue()
      organizationRoleService
        .findType(testData.granularPermissionUser.id, testData.otherOrg.id)
        .assert
        .isNull()
    }
  }

  @Test
  fun `direct permission holder sees no role on the org endpoint`() {
    userAccount = testData.directPermissionUser
    performAuthGet("/v2/organizations/${testData.otherOrg.id}").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
    }
  }

  @Test
  fun `a direct-permission holder can leave the project`() {
    userAccount = testData.guestWithPermission
    performAuthPut("/v2/projects/${testData.otherOrgPrivateProject.id}/leave", null).andIsOk
    performAuthGet("/v2/projects/${testData.otherOrgPrivateProject.id}").andIsNotFound
  }

  @Test
  fun `a NONE permission grants standing, reports no role and the reduced model`() {
    executeInNewTransaction {
      userPreferencesService.setPreferredOrganization(
        organizationService.get(testData.otherOrg.id),
        userAccountService.get(testData.noneOnlyUser.id),
      )
    }
    userAccount = testData.noneOnlyUser
    // A NONE permission is still a permission row (standing), so the user is NOT a pure community
    // viewer — limitedView is false — but they are below MEMBER, so the model is still reduced.
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
      node("activeCloudSubscription").isEqualTo(null)
    }
  }

  @Test
  fun `a floor viewer leaving the org 404s from the service, not access denial`() {
    userAccount = testData.nonMember
    performAuthPut("/v2/organizations/${testData.otherOrg.id}/leave", null)
      .andIsNotFound
      .andAssertThatJson {
        node("code").isEqualTo("user_is_not_member_of_organization")
      }
  }

  @Test
  fun `leave for a revoked-permission user removes the revoked tie`() {
    userAccount = testData.revokedOnlyUser
    performAuthPut("/v2/organizations/${testData.noPublicOrg.id}/leave", null).andIsOk
    executeInNewTransaction {
      organizationRoleService
        .canUserViewStrictOrPublic(testData.revokedOnlyUser.id, testData.noPublicOrg.id)
        .assert
        .isFalse()
    }
  }

  @Test
  fun `a revoked NONE permission on a private-only org yields the reduced model`() {
    executeInNewTransaction {
      userPreferencesService.setPreferredOrganization(
        organizationService.get(testData.noPublicOrg.id),
        userAccountService.get(testData.revokedOnlyUser.id),
      )
    }
    userAccount = testData.revokedOnlyUser
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("id").isEqualTo(testData.noPublicOrg.id)
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
      node("activeCloudSubscription").isEqualTo(null)
    }
  }

  @Test
  fun `a NONE permission keeps org access and reports no role on the org endpoint`() {
    userAccount = testData.revokedOnlyUser
    performAuthGet("/v2/organizations/${testData.noPublicOrg.id}").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
    }
  }

  @Test
  fun `a role-less server admin gets the full model with limitedView false`() {
    executeInNewTransaction {
      userPreferencesService.setPreferredOrganization(
        organizationService.get(testData.otherOrg.id),
        userAccountService.get(testData.serverAdmin.id),
      )
    }
    userAccount = testData.serverAdmin
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
    }
  }

  @Test
  fun `a public-project floor viewer does not leak into org member management`() {
    userAccount = testData.otherOrgOwner
    val response =
      performAuthGet("/v2/organizations/${testData.otherOrg.id}/users?size=50").andIsOk.andReturn()
    val usernames =
      jacksonObjectMapper()
        .readTree(response.response.contentAsString)
        .path("_embedded")
        .path("usersInOrganization")
        .map { it.path("username").asText() }
    // stored_guest has neither an org role nor a project permission — only public-project floor
    // access — so it must never surface in org member management (it would count as a paid seat).
    usernames.assert.doesNotContain("stored_guest")
  }

  @Test
  fun `project member listing excludes floor viewers`() {
    userAccount = testData.otherOrgOwner
    performAuthGet("/v2/projects/${testData.otherOrgPublicProject.id}/users").andIsOk.andAssertThatJson {
      node("_embedded.users").isArray.hasSize(3)
    }
  }

  @Test
  fun `project stats member count excludes floor viewers`() {
    userAccount = testData.otherOrgOwner
    performAuthGet("/v2/projects/${testData.otherOrgPublicProject.id}/stats").andIsOk.andAssertThatJson {
      node("membersCount").isEqualTo(3)
    }
  }

  @Test
  fun `permitted-user search excludes floor viewers`() {
    executeInNewTransaction {
      val users =
        userAccountService.findWithMinimalPermissions(
          UserAccountPermissionsFilters(),
          testData.otherOrgPublicProject.id,
          search = null,
          pageable = PageRequest.of(0, 20),
        )
      users.content
        .map { it.username }
        .assert
        .containsExactlyInAnyOrder("other_org_member", "other_org_owner", "direct_perm_user")
    }
  }

  @Test
  fun `a role in a soft-deleted organization grants no standing`() {
    executeInNewTransaction {
      organizationRoleService
        .canUserViewStrictOrPublic(testData.softDeletedOrgMember.id, testData.softDeletedOrg.id)
        .assert
        .isFalse()
    }
  }

  @Test
  fun `direct permission in a soft-deleted organization grants no standing`() {
    executeInNewTransaction {
      organizationService.delete(organizationService.get(testData.otherOrg.id))
    }
    executeInNewTransaction {
      organizationRoleService
        .canUserViewStrictOrPublic(testData.directPermissionUser.id, testData.otherOrg.id)
        .assert
        .isFalse()
    }
  }
}
