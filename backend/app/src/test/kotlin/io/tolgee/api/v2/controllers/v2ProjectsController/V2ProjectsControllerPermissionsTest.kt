package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
open class V2ProjectsControllerPermissionsTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  fun setUsersPermissions() {
    withPermissionsTestData { project, user ->
      performAuthPut("/v2/projects/${project.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

      permissionService.getProjectPermissionScopes(project.id, user)
        .let { Assertions.assertThat(it).equalsPermissionType(ProjectPermissionType.EDIT) }
    }
  }

  @Test
  fun `sets user's permissions with languages`() {
    checkSetPermissionsWithLanguages({ getLang ->
      "languages=${getLang("en")}&" +
        "languages=${getLang("de")}"
    }, { data, getLang ->
      Assertions.assertThat(data.computedPermissions.translateLanguageIds).contains(getLang("en"))
      Assertions.assertThat(data.computedPermissions.translateLanguageIds).contains(getLang("de"))
      Assertions.assertThat(data.computedPermissions.viewLanguageIds).isEmpty()
      Assertions.assertThat(data.computedPermissions.stateChangeLanguageIds).isEmpty()
    })
  }

  @Test
  fun `sets user's permissions with translateLanguages and view `() {
    checkSetPermissionsWithLanguages({ getLang ->
      "translateLanguages=${getLang("en")}&" +
        "translateLanguages=${getLang("de")}&" +
        "viewLanguages=${getLang("de")}&" +
        "stateChangeLanguages=${getLang("en")}"
    }, { data, getLangId ->
      Assertions.assertThat(data.computedPermissions.translateLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"), getLangId("de"))
      Assertions.assertThat(data.computedPermissions.viewLanguageIds)
        .containsExactlyInAnyOrder(getLangId("de"))
      Assertions.assertThat(data.computedPermissions.stateChangeLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"))
    })
  }

  private fun checkSetPermissionsWithLanguages(
    getQueryFn: (langByTag: LangByTag) -> String,
    checkFn: (data: ProjectPermissionData, langByTag: LangByTag) -> Unit
  ) {
    withPermissionsTestData { project, user ->
      val languages = project.languages.toList()
      val langByTag = { tag: String -> languages.find { it.tag == tag }!!.id }
      val query = getQueryFn(langByTag)

      performAuthPut(
        "/v2/projects/${project.id}/users/${user.id}" +
          "/set-permissions/TRANSLATE?$query",
        null
      ).andIsOk

      permissionService.getProjectPermissionData(project.id, user.id)
        .let {
          Assertions.assertThat(it.computedPermissions.scopes).containsAll(
            ProjectPermissionType.TRANSLATE.availableScopes.toList()
          )
          checkFn(it, langByTag)
        }
    }
  }

  fun withPermissionsTestData(fn: (project: Project, user: UserAccount) -> Unit) {
    val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
    val project = usersAndOrganizations[1].organizationRoles[0].organization!!.projects[0]
    val user = dbPopulator.createUserIfNotExists("jirina")
    organizationRoleService.grantMemberRoleToUser(user, project.organizationOwner)

    permissionService.create(Permission(user = user, project = project, type = ProjectPermissionType.VIEW))

    loginAsUser(usersAndOrganizations[1].name)
    fn(project, user)
  }
}
