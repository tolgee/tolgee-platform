package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerMembersTest : BaseOrganizationControllerTest() {
  @Test
  fun testGetAllUsers() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[0].username)
    val organizationId = users[1].organizationRoles[0].organization!!.id
    performAuthGet("/v2/organizations/$organizationId/users")
      .andIsOk
      .also { println(it.andReturn().response.contentAsString) }
      .andAssertThatJson {
        node("_embedded.usersInOrganization") {
          isArray.hasSize(2)
          node("[0].organizationRole").isEqualTo("MEMBER")
          node("[1].organizationRole").isEqualTo("OWNER")
        }
      }
  }

  @Test
  fun `it returns also users with direct permissions on some project`() {
    val testData = PermissionsTestData()
    testData.addUserWithPermissions(type = ProjectPermissionType.MANAGE)
    testData.addUnrelatedUsers()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin.self

    performAuthGet("/v2/organizations/${testData.organizationBuilder.self.id}/users")
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.usersInOrganization") {
          isArray.hasSize(4)
          node("[0]") {
            node("organizationRole").isEqualTo("OWNER")
            node("username").isEqualTo("admin@admin.com")
            node("projectsWithDirectPermission").isArray.isEmpty()
          }
          node("[1]") {
            node("organizationRole").isEqualTo(null)
            node("username").isEqualTo("member@member.com")
            node("projectsWithDirectPermission") {
              isArray.hasSize(1)
              node("[0]") {
                node("id").isNumber
                node("name").isEqualTo("Project")
              }
            }
          }
          node("[2]") {
            node("organizationRole").isEqualTo(null)
            node("username").isEqualTo("me@me.me")
            node("projectsWithDirectPermission").isArray.hasSize(1)
          }
          node("[3]") {
            node("organizationRole").isEqualTo("MEMBER")
            node("username").isEqualTo("org@org.org")
            node("projectsWithDirectPermission").isArray.isEmpty()
          }
        }
      }
  }

  @Test
  fun testGetAllUsersNotPermitted() {
    val users = dbPopulator.createUsersAndOrganizations()
    val organizationId = users[1].organizationRoles[0].organization!!.id
    performAuthGet("/v2/organizations/$organizationId/users").andIsNotFound
  }

  @Test
  @Transactional
  fun `cannot set own permission`() {
    withOwnerInOrganization { organization, owner, role ->
      loginAsUser(owner)
      performAuthPut(
        "/v2/organizations/${organization.id}/users/${owner.id}/set-role",
        SetOrganizationRoleDto(OrganizationRoleType.MEMBER),
      ).andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_YOUR_OWN_ROLE)
    }
  }

  @AfterEach
  fun resetProps() {
    tolgeeProperties.authentication.userCanCreateOrganizations = true
  }

  @Test
  fun testRemoveUser() {
    withOwnerInOrganization { organization, owner, role ->
      organizationRoleRepository.save(role)
      performAuthDelete("/v2/organizations/${organization.id}/users/${owner.id}", null).andIsOk
      organizationRoleRepository.findByIdOrNull(role.id).let {
        assertThat(it).isNull()
      }
    }
  }

  @Test
  fun `removes user with all permissions`() {
    val testData = PermissionsTestData()
    val me = testData.addUserWithPermissions(type = ProjectPermissionType.MANAGE)
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin.self
    permissionService
      .getProjectPermissionData(
        testData.projectBuilder.self.id,
        me.id,
      ).directPermissions.assert.isNotNull
    performAuthDelete("/v2/organizations/${testData.organizationBuilder.self.id}/users/${me.id}", null)
      .andIsOk
    permissionService
      .getProjectPermissionData(testData.projectBuilder.self.id, me.id)
      .directPermissions.assert
      .isNull()
  }

  @Test
  fun `remove user resets preferred`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.kvetoslav
    performAuthDelete("/v2/organizations/${testData.jirinaOrg.id}/users/${testData.jirina.id}").andIsOk

    assertThat(userPreferencesService.find(testData.jirina.id)!!.preferredOrganization!!.id)
      .isNotEqualTo(testData.jirinaOrg.id)
  }

  @Test
  fun `doesn't create new preferred when cannot create organizations`() {
    tolgeeProperties.authentication.userCanCreateOrganizations = false
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.franta

    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isString.isEqualTo("test_username")
    }

    assertThat(userPreferencesService.find(testData.franta.id)!!.preferredOrganization)
      .isNotNull

    userAccount = testData.user

    performAuthPut(
      "/v2/projects/${testData.projectBuilder.self.id}/users/${testData.franta.id}/revoke-access",
      null,
    ).andIsOk

    assertThat(userPreferencesService.find(testData.franta.id)!!.preferredOrganization)
      .isNull()

    userAccount = testData.franta

    performAuthGet("/v2/preferred-organization").andIsForbidden
  }
}
