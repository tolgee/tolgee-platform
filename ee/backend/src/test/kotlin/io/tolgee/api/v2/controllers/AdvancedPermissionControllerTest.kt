package io.tolgee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Organization
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.PermissionTestUtil
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.ResultActions

class AdvancedPermissionControllerTest : AuthorizedControllerTest() {

  private val permissionTestUtil: PermissionTestUtil by lazy { PermissionTestUtil(this, applicationContext) }

  @Test
  fun `sets user's permissions with translateLanguages and view `() {
    permissionTestUtil.checkSetPermissionsWithLanguages("", { getLang ->
      "translateLanguages=${getLang("en")}&" +
        "translateLanguages=${getLang("de")}&" +
        "viewLanguages=${getLang("de")}&" +
        "stateChangeLanguages=${getLang("en")}" +
        "&scopes=translations.edit&scopes=translations.state-edit&"
    }) { data, getLangId ->
      Assertions.assertThat(data.computedPermissions.translateLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"), getLangId("de"))
      Assertions.assertThat(data.computedPermissions.viewLanguageIds)
        .containsExactlyInAnyOrder(getLangId("de"), getLangId("en"))
      Assertions.assertThat(data.computedPermissions.stateChangeLanguageIds)
        .containsExactlyInAnyOrder(getLangId("en"))
    }
  }

  @Test
  fun `validates permissions (view languages and scopes)`() {
    permissionTestUtil.performSetPermissions(
      ""
    ) { getLang -> "scopes=screenshots.upload&viewLanguages=${getLang("en")}" }
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_SET_VIEW_LANGUAGES_WITHOUT_TRANSLATIONS_VIEW_SCOPE)
  }

  @Test
  fun `validates permissions (translate languages and scopes)`() {
    permissionTestUtil.performSetPermissions(
      ""
    ) { getLang -> "scopes=translations.view&translateLanguages=${getLang("en")}" }
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_SET_TRANSLATE_LANGUAGES_WITHOUT_TRANSLATIONS_EDIT_SCOPE)
  }

  @Test
  fun `validates permissions (state change languages and scopes)`() {
    permissionTestUtil.performSetPermissions(
      ""
    ) { getLang -> "scopes=translations.view&stateChangeLanguages=${getLang("en")}" }
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_SET_STATE_CHANGE_LANGUAGES_WITHOUT_TRANSLATIONS_STATE_EDIT_SCOPE)
  }

  @Test
  fun `validates permissions (empty scopes)`() {
    permissionTestUtil.performSetPermissions(
      ""
    ) { "" }
      .andIsBadRequest
      .andHasErrorMessage(Message.SCOPES_HAS_TO_BE_SET)
  }

  @Test
  fun `validates permissions (admin and viewLanguages)`() {
    permissionTestUtil.performSetPermissions("") { getLang ->
      "scopes=admin&viewLanguages=${getLang("en")}"
    }.andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
  }

  @Test
  fun `validates permissions (admin and translateLanguages)`() {
    permissionTestUtil.performSetPermissions("") { getLang ->
      "scopes=admin&translateLanguages=${getLang("en")}"
    }.andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
  }

  @Test
  fun `validates permissions (admin and stateChangeLanguages)`() {
    permissionTestUtil.performSetPermissions("") { getLang ->
      "scopes=admin&stateChangeLanguages=${getLang("en")}"
    }.andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
  }

  @Test
  fun `sets organization base permission with scopes`() {
    permissionTestUtil.withPermissionsTestData { project, user ->
      val org = project.organizationOwner
      performSetScopesBaseOrganization(org).andIsOk

      organizationService.get(org.id).basePermission.scopes.assert.containsExactlyInAnyOrder(
        Scope.TRANSLATIONS_EDIT, Scope.TRANSLATIONS_STATE_EDIT
      )
    }
  }

  @Test
  fun `goes from scopes to type and from type to scopes`() {
    permissionTestUtil.withPermissionsTestData { project, user ->
      val org = project.organizationOwner
      performSetScopesBaseOrganization(org).andIsOk

      performAuthPut(
        "/v2/organizations/${org.id}/set-base-permissions/TRANSLATE",
        null
      ).andIsOk

      performSetScopesBaseOrganization(org).andIsOk
    }
  }

  private fun performSetScopesBaseOrganization(org: Organization): ResultActions {
    return performAuthPut(
      "/v2/organizations/${org.id}/set-base-permissions?scopes=translations.edit&scopes=translations.state-edit",
      null
    )
  }
}
