package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.development.testDataBuilder.data.ProjectsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isPermissionScopes
import io.tolgee.fixtures.node
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.WithoutEeTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
@WithoutEeTest
class ProjectsControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  fun getAll() {
    executeInNewTransaction {
      dbPopulator.createBase("kim")
      dbPopulator.createBase("kim")

      loginAsUser("kim")

      dbPopulator.createOrganization("cool", userAccount!!).let { org ->
        dbPopulator.createProjectWithOrganization("org repo", org)
      }
    }
    performAuthGet("/v2/projects").andPrettyPrint.andAssertThatJson.node("_embedded.projects").let {
      it.isArray.hasSize(3)
      it.node("[0].organizationOwner.name").isEqualTo("kim")
      it.node("[2].organizationOwner.name").isEqualTo("cool")
      it.node("[2].organizationOwner.slug").isEqualTo("cool")
      it.node("[2].icuPlaceholders").isEqualTo(true)
    }
  }

  @Test
  fun `get all has language permissions`() {
    val baseTestData = BaseTestData()

    var franta: UserAccount? = null
    baseTestData.root.apply {
      franta = addUserAccount { username = "franta" }.self
      data.projects[0].addPermission {
        user = franta
        type = ProjectPermissionType.TRANSLATE
        translateLanguages = mutableSetOf(baseTestData.englishLanguage)
      }
    }

    testDataService.saveTestData(baseTestData.root)
    userAccount = franta

    performAuthGet("/v2/projects").andPrettyPrint.andAssertThatJson.node("_embedded.projects").let {
      it.isArray.hasSize(1)
      it
        .node("[0].computedPermission.permittedLanguageIds")
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
      .andIsOk
      .andAssertThatJson {
        node("_embedded.projects") {
          isArray.hasSize(2)
          node("[0].organizationOwner.name").isEqualTo("test_username")
          node("[0].directPermission.scopes").isPermissionScopes(ProjectPermissionType.MANAGE)
          node("[0].computedPermission.scopes").isPermissionScopes(ProjectPermissionType.MANAGE)
          node("[0].stats.translationStatePercentages").isEqualTo(
            """
        {
          "UNTRANSLATED": 100.0,
          "TRANSLATED": 0,
          "REVIEWED": 0
        }
      """,
          )
          node("[1].stats.translationStatePercentages").isEqualTo(
            """
        {
          "UNTRANSLATED": 25.0,
          "TRANSLATED": 75.0,
          "REVIEWED": 0.0
        }
      """,
          )
        }
      }
  }

  @Test
  fun `with-stats returns permitted languages`() {
    val testData = ProjectsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userWithTranslatePermission

    performAuthGet("/v2/projects/with-stats?sort=id")
      .andIsOk.andAssertThatJson
      .node("_embedded.projects")
      .let {
        it.isArray.hasSize(1)
        it.node("[0].computedPermission.permittedLanguageIds").isArray.hasSize(2).containsAll(
          mutableListOf(
            testData.project2English.id,
            testData.project2Deutsch.id,
          ),
        )
      }
  }

  @Test
  fun `get single returns permissions`() {
    val base = dbPopulator.createBase()
    userAccount = dbPopulator.createUserIfNotExists("another-user")
    permissionService.create(
      Permission(
        project = base.project,
        user = userAccount,
        type = ProjectPermissionType.TRANSLATE,
      ).apply { translateLanguages = mutableSetOf(base.project.languages.first()) },
    )

    performAuthGet("/v2/projects/${base.project.id}").andPrettyPrint.andAssertThatJson.let {
      it.node("organizationOwner.name").isEqualTo(base.userAccount.username)
      it.node("directPermission.scopes").isPermissionScopes(ProjectPermissionType.TRANSLATE)
      it
        .node("computedPermission.permittedLanguageIds")
        .isArray
        .hasSize(1)
        .contains(
          base.project.languages
            .first()
            .id,
        )
    }
  }

  @Test
  fun getNotPermitted() {
    val base = dbPopulator.createBase()

    val account = dbPopulator.createUserIfNotExists("peter")
    loginAsUser(account.name)

    performAuthGet("/v2/projects/${base.project.id}").andIsNotFound
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
        type = ProjectPermissionType.TRANSLATE
        translateLanguages = project!!.languages.toMutableSet()
      },
    )

    loginAsUser(usersAndOrganizations[1].name)

    performAuthGet("/v2/projects/${directPermissionProject.id}/users?sort=id")
      .andIsOk.andPrettyPrint.andAssertThatJson
      .node("_embedded.users")
      .let {
        it.isArray.hasSize(3)
        it.node("[0].organizationRole").isEqualTo("MEMBER")
        it.node("[1].organizationRole").isEqualTo("OWNER")
        it.node("[2].directPermission.scopes").isPermissionScopes(ProjectPermissionType.TRANSLATE)
        it
          .node("[2].computedPermission.permittedLanguageIds")
          .isArray
          .hasSize(2)
          .containsAll(directPermissionProject.languages.map { it.id })
      }
  }

  @Test
  fun setUsersPermissionsNoAccess() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/set-permissions/EDIT", null)
      .andIsBadRequest
      .andReturn()
      .let {
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
      .andIsBadRequest
      .andReturn()
      .let {
        assertThat(it).error().hasCode("user_is_organization_owner")
      }
  }

  @Test
  fun setUsersPermissionsOwn() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo =
      usersAndOrganizations[1]
        .organizationRoles[0]
        .organization!!
        .projects[0]

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${usersAndOrganizations[1].id}/set-permissions/EDIT", null)
      .andIsBadRequest
      .andReturn()
      .let {
        assertThat(it).error().hasCode("cannot_set_your_own_permissions")
      }
  }

  @Test
  fun revokeUsersAccess() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    permissionService.create(Permission(user = user, project = repo, type = ProjectPermissionType.VIEW))

    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/revoke-access", null).andIsOk

    permissionService
      .getProjectPermissionScopesNoApiKey(repo.id, user)
      .let { assertThat(it).isEmpty() }
  }

  @Test
  fun revokeUsersAccessOwn() {
    val base = dbPopulator.createBase("jirina")

    loginAsUser("jirina")

    performAuthPut("/v2/projects/${base.project.id}/users/${base.userAccount.id}/revoke-access", null)
      .andIsBadRequest
      .andReturn()
      .let { assertThat(it).error().hasCode("can_not_revoke_own_permissions") }
  }

  @Test
  fun revokeUsersAccessIsOrganizationMember() {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")

    organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner)
    loginAsUser(usersAndOrganizations[1].name)

    performAuthPut("/v2/projects/${repo.id}/users/${user.id}/revoke-access", null)
      .andIsBadRequest
      .andReturn()
      .let { assertThat(it).error().hasCode("user_is_organization_member") }
  }

  @Test
  fun deleteProject() {
    val base = dbPopulator.createBase()
    performAuthDelete("/v2/projects/${base.project.id}", null).andIsOk
    val project = projectService.find(base.project.id)
    assertThat(project).isNull()
  }

  @Test
  fun `test bad request error is returned when requested url with wrong pattern`() {
    performAuthGet("/v2/projects/export&format=JSON")
      .andIsNotFound
      .andPrettyPrint
      .andAssertThatJson
      .let {
        it.node("code").isEqualTo(Message.RESOURCE_NOT_FOUND.code)
        it.node("params[0]").isEqualTo("v2/projects/export&format=JSON")
      }
  }
}
