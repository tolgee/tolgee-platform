package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.development.testDataBuilder.data.ProjectsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
open class V2ProjectsControllerTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `get all has language permissions`() {
    val baseTestData = BaseTestData()
    baseTestData.root.apply {
      data.projects[0].data.permissions[0].self.languages = mutableSetOf(baseTestData.englishLanguage)
    }
    testDataService.saveTestData(baseTestData.root)

    userAccount = baseTestData.user

    performAuthGet("/v2/projects").andPrettyPrint.andAssertThatJson.node("_embedded.projects").let {
      it.isArray.hasSize(1)
      it.node("[0].computedPermissions.permittedLanguageIds")
        .isArray
        .hasSize(1)
        .containsAll(listOf(baseTestData.englishLanguage.id))
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
  fun `with-stats returns permitted languages`() {
    val testData = ProjectsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userWithTranslatePermission

    performAuthGet("/v2/projects/with-stats?sort=id")
      .andIsOk.andAssertThatJson.node("_embedded.projects").let {
        it.isArray.hasSize(1)
        it.node("[0].computedPermissions.permittedLanguageIds").isArray.hasSize(2).containsAll(
          mutableListOf(
            testData.project2English.id,
            testData.project2Deutsch.id
          )
        )
      }
  }

  @Test
  fun get() {
    val base = dbPopulator.createBase("one")

    val permission = base.permissions.first()
    permission.languages = mutableSetOf(base.languages.first())

    permissionService.saveAll(listOf(permission))

    performAuthGet("/v2/projects/${base.id}").andPrettyPrint.andAssertThatJson.let {
      it.node("userOwner.name").isEqualTo("admin")
      it.node("directPermissions").isEqualTo("MANAGE")
      it.node("computedPermissions.permittedLanguageIds").isArray.hasSize(1).contains(base.languages.first().id)
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
    val directPermissionProject = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]

    val directPermissionUser = dbPopulator.createUserIfNotExists("jirina")
    permissionService.create(
      Permission().apply {
        user = directPermissionUser
        project = directPermissionProject
        type = Permission.ProjectPermissionType.TRANSLATE
        languages = project.languages.toMutableSet()
      }
    )

    loginAsUser(usersAndOrganizations[1].name)

    performAuthGet("/v2/projects/${directPermissionProject.id}/users?sort=id")
      .andIsOk.andPrettyPrint.andAssertThatJson
      .node("_embedded.users").let {
        it.isArray.hasSize(3)
        it.node("[0].organizationRole").isEqualTo("MEMBER")
        it.node("[1].organizationRole").isEqualTo("OWNER")
        it.node("[2].directPermissions").isEqualTo("TRANSLATE")
        it.node("[2].computedPermissions.permittedLanguageIds")
          .isArray
          .hasSize(2)
          .containsAll(directPermissionProject.languages.map { it.id })
      }
  }

  @Test
  fun setUsersPermissions() {
    withPermissionsTestData { project, user ->
      performAuthPut("/v2/projects/${project.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

      permissionService.getProjectPermissionType(project.id, user)
        .let { assertThat(it).isEqualTo(Permission.ProjectPermissionType.EDIT) }
    }
  }

  @Test
  fun `sets user's permissions with languages`() {
    withPermissionsTestData { project, user ->
      val languages = project.languages.toList()
      val lng1 = languages[0]
      val lng2 = languages[1]

      performAuthPut(
        "/v2/projects/${project.id}/users/${user.id}" +
          "/set-permissions/TRANSLATE?" +
          "languages=${lng1.id}&" +
          "languages=${lng2.id}",
        null
      ).andIsOk

      permissionService.getProjectPermissionData(project.id, user.id)
        .let {
          assertThat(it.computedPermissions.type).isEqualTo(Permission.ProjectPermissionType.TRANSLATE)
          assertThat(it.computedPermissions.languageIds).contains(lng1.id)
          assertThat(it.computedPermissions.languageIds).contains(lng2.id)
        }
    }
  }

  @Test
  fun setUsersPermissionsDeletesPermission() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val project = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    organizationRoleService.grantMemberRoleToUser(user, project.organizationOwner!!)
    permissionService.create(Permission(user = user, project = project, type = Permission.ProjectPermissionType.VIEW))
    project.organizationOwner!!.basePermissions = Permission.ProjectPermissionType.EDIT
    organizationRepository.save(project.organizationOwner!!)

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${project.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

    permissionService.getProjectPermissionData(project.id, user.id)
      .let { assertThat(it.directPermissions).isEqualTo(null) }
  }

  fun withPermissionsTestData(fn: (project: Project, user: UserAccount) -> Unit) {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val project = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")
    organizationRoleService.grantMemberRoleToUser(user, project.organizationOwner!!)

    permissionService.create(Permission(user = user, project = project, type = Permission.ProjectPermissionType.VIEW))

    loginAsUser(usersAndOrganizations[1].name)
    fn(project, user)
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
}
