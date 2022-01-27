package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ProjectsTestData
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Permission
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class V2ProjectsControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  fun getAll() {
    dbPopulator.createBase("one", "kim")
    dbPopulator.createBase("two", "kim")

    loginAsUser("kim")

    dbPopulator.createOrganization("cool", userAccount!!).let { org ->
      dbPopulator.createProjectWithOrganization("org repo", org)
    }

    performAuthGet("/v2/projects").andPrettyPrint.andAssertThatJson.node("_embedded.projects").let {
      it.isArray.hasSize(3)
      it.node("[0].userOwner.name").isEqualTo("kim")
      it.node("[0].directPermissions").isEqualTo("MANAGE")
      it.node("[2].userOwner").isEqualTo("null")
      it.node("[2].organizationOwnerName").isEqualTo("cool")
      it.node("[2].organizationOwnerSlug").isEqualTo("cool")
    }
  }

  @Test
  fun getAllWithStats() {
    val testData = ProjectsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user

    performAuthGet("/v2/projects/with-stats?sort=id")
      .andIsOk.andAssertThatJson.node("_embedded.projects").let {
        it.isArray.hasSize(2)
        it.node("[0].userOwner.username").isEqualTo("test_username")
        it.node("[0].directPermissions").isEqualTo("MANAGE")
        it.node("[1].stats.translationStateCounts").isEqualTo(
          """
        {
          "UNTRANSLATED": 4,
          "TRANSLATED": 5,
          "REVIEWED": 1
        }
      """
        )
        it.node("[0].stats.translationStateCounts").isEqualTo(
          """
        {
          "UNTRANSLATED": 1,
          "TRANSLATED": 0,
          "REVIEWED": 0
        }
      """
        )
      }
  }

  @Test
  fun get() {
    val base = dbPopulator.createBase("one")

    performAuthGet("/v2/projects/${base.id}").andPrettyPrint.andAssertThatJson.let {
      it.node("userOwner.name").isEqualTo("admin")
      it.node("directPermissions").isEqualTo("MANAGE")
    }
  }

  @Test
  fun getNotPermitted() {
    val base = dbPopulator.createBase("one")

    val account = dbPopulator.createUserIfNotExists("peter")
    loginAsUser(account.name)

    performAuthGet("/v2/projects/${base.id}").andIsForbidden
  }

  @Test
  fun getAllUsers() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")
    permissionService.grantFullAccessToProject(user, repo)

    loginAsUser(usersAndOrganizations[1].name)

    performAuthGet("/v2/projects/${repo.id}/users?sort=id").andPrettyPrint.andAssertThatJson
      .node("_embedded.users").let {
        it.isArray.hasSize(3)
        it.node("[0].organizationRole").isEqualTo("MEMBER")
        it.node("[1].organizationRole").isEqualTo("OWNER")
        it.node("[2].directPermissions").isEqualTo("MANAGE")
      }
  }

  @Test
  fun setUsersPermissions() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    permissionService.create(Permission(user = user, project = repo, type = Permission.ProjectPermissionType.VIEW))

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

    permissionService.getProjectPermissionType(repo.id, user)
      .let { assertThat(it).isEqualTo(Permission.ProjectPermissionType.EDIT) }
  }

  @Test
  fun setUsersPermissionsDeletesPermission() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner!!)
    permissionService.create(Permission(user = user, project = repo, type = Permission.ProjectPermissionType.VIEW))

    repo.organizationOwner!!.basePermissions = Permission.ProjectPermissionType.EDIT
    organizationRepository.save(repo.organizationOwner!!)

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

    permissionService.getProjectPermissionData(repo.id, user.id)
      .let { assertThat(it.directPermissions).isEqualTo(null) }
  }

  @Test
  fun setUsersPermissionsNoAccess() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/set-permissions/EDIT", null)
      .andIsBadRequest.andReturn().let {
        assertThat(it).error().hasCode("user_has_no_project_access")
      }
  }

  @Test
  fun setUsersPermissionsOwner() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")
    organizationRoleService.grantOwnerRoleToUser(user, repo.organizationOwner!!)

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/set-permissions/EDIT", null)
      .andIsBadRequest.andReturn().let {
        assertThat(it).error().hasCode("user_is_organization_owner")
      }
  }

  @Test
  fun setUsersPermissionsHigherBase() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")
    organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner!!)

    repo.organizationOwner!!.basePermissions = Permission.ProjectPermissionType.EDIT
    organizationRepository.save(repo.organizationOwner!!)

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/set-permissions/TRANSLATE", null)
      .andIsBadRequest.andReturn().let {
        assertThat(it).error().hasCode("cannot_set_lower_than_organization_base_permissions")
      }
  }

  @Test
  fun setUsersPermissionsOwn() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${usersAndOrganizations[1].id}/set-permissions/EDIT", null)
      .andIsBadRequest.andReturn().let {
        assertThat(it).error().hasCode("cannot_set_your_own_permissions")
      }
  }

  @Test
  fun revokeUsersAccess() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    permissionService.create(Permission(user = user, project = repo, type = Permission.ProjectPermissionType.VIEW))

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/revoke-access", null).andIsOk

    permissionService.getProjectPermissionType(repo.id, user)
      .let { assertThat(it).isNull() }
  }

  @Test
  fun revokeUsersAccessOwn() {
    val repo = dbPopulator.createBase("base", "jirina")

    loginAsUser("jirina")

    performAuthPut("/v2/projects/${repo.id}/users/${repo.userOwner!!.id}/revoke-access", null)
      .andIsBadRequest.andReturn().let { assertThat(it).error().hasCode("can_not_revoke_own_permissions") }
  }

  @Test
  fun revokeUsersAccessIsOrganizationMember() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner!!)
    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/revoke-access", null)
      .andIsBadRequest.andReturn().let { assertThat(it).error().hasCode("user_is_organization_member") }
  }

  @Test
  fun deleteProject() {
    val base = dbPopulator.createBase(generateUniqueString())
    performAuthDelete("/v2/projects/${base.id}", null).andIsOk
    val project = projectService.find(base.id)
    Assertions.assertThat(project).isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun inviteUserToProject() {
    val key = performProjectAuthPut("/invite", ProjectInviteUserDto(Permission.ProjectPermissionType.MANAGE))
      .andIsOk.andGetContentAsString
    assertThat(key).hasSize(50)
  }
}
