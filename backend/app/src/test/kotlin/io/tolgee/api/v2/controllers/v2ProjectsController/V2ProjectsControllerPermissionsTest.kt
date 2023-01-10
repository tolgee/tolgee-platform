package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.PermissionTestUtil
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class V2ProjectsControllerPermissionsTest : ProjectAuthControllerTest("/v2/projects/") {

  private val permissionTestUtil: PermissionTestUtil by lazy { PermissionTestUtil(this, applicationContext) }

  @Test
  fun `sets user permissions`() {
    permissionTestUtil.withPermissionsTestData { project, user ->
      performAuthPut("/v2/projects/${project.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

      permissionService.getProjectPermissionScopes(project.id, user)
        .let { Assertions.assertThat(it).equalsPermissionType(ProjectPermissionType.EDIT) }
    }
  }

  @Test
  fun `sets user's permissions with languages`() {
    permissionTestUtil.checkSetPermissionsWithLanguages("TRANSLATE", { getLang ->
      "languages=${getLang("en")}&" +
        "languages=${getLang("de")}"
    }) { data, getLang ->
      Assertions.assertThat(data.computedPermissions.translateLanguageIds).contains(getLang("en"))
      Assertions.assertThat(data.computedPermissions.translateLanguageIds).contains(getLang("de"))
      Assertions.assertThat(data.computedPermissions.viewLanguageIds).containsExactlyInAnyOrder(
        getLang("en"),
        getLang("de")
      )
      Assertions.assertThat(data.computedPermissions.stateChangeLanguageIds).isEmpty()
    }
  }

  @Test
  fun `sets user's permissions with translateLanguages and review `() {
    permissionTestUtil.checkSetPermissionsWithLanguages("REVIEW", { getLang ->
      "translateLanguages=${getLang("en")}&" +
        "translateLanguages=${getLang("de")}&" +
        "viewLanguages=${getLang("de")}&" +
        "stateChangeLanguages=${getLang("en")}"
    }) { data, getLangId ->
      Assertions.assertThat(data.computedPermissions.scopes).containsAll(
        ProjectPermissionType.TRANSLATE.availableScopes.toList()
      )
      Assertions.assertThat(data.computedPermissions.translateLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"), getLangId("de"))
      Assertions.assertThat(data.computedPermissions.viewLanguageIds)
        .containsExactlyInAnyOrder(getLangId("de"), getLangId("en"))
      Assertions.assertThat(data.computedPermissions.stateChangeLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"))
    }
  }

  @Test
  fun `sets user's permissions with translateLanguages and view only`() {
    permissionTestUtil.checkSetPermissionsWithLanguages("TRANSLATE", { getLang ->
      "translateLanguages=${getLang("de")}&" +
        "viewLanguages=${getLang("en")}&"
    }) { data, getLangId ->
      Assertions.assertThat(data.computedPermissions.viewLanguageIds)
        .containsExactlyInAnyOrder(getLangId("de"), getLangId("en"))
    }
  }

  @Test
  fun `view contains at least the scopes from translate and state change`() {
    permissionTestUtil.checkSetPermissionsWithLanguages("REVIEW", { getLang ->
      "translateLanguages=${getLang("en")}&" +
        "stateChangeLanguages=${getLang("de")}"
    }) { data, getLangId ->
      Assertions.assertThat(data.computedPermissions.viewLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"), getLangId("de"))
    }
  }

  @Test
  fun `cannot save stateChangeLanguages when translate`() {
    permissionTestUtil.performSetPermissions("TRANSLATE") { getLang ->
      "stateChangeLanguages=${getLang("de")}"
    }.andIsBadRequest
  }

  @Test
  fun `cannot save viewLanguages when none`() {
    permissionTestUtil.performSetPermissions("NONE") { getLang ->
      "viewLanguages=${getLang("de")}"
    }.andIsBadRequest
  }

  @Test
  fun `cannot save stateChangeLanguages when view`() {
    permissionTestUtil.performSetPermissions("VIEW") { getLang ->
      "stateChangeLanguages=${getLang("de")}"
    }.andIsBadRequest
  }

  @Test
  fun `cannot save translationEditLangueges when view`() {
    permissionTestUtil.performSetPermissions("VIEW") { getLang ->
      "translateLanguages=${getLang("de")}"
    }.andIsBadRequest
  }
}
