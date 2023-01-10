package io.tolgee.api.v2.controllers

import io.tolgee.constants.Message
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.PermissionTestUtil
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test

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
  fun `validates permissions (languages and scopes)`() {
    permissionTestUtil.performSetPermissions(
      ""
    ) { getLang -> "scopes=translations.view&translateLanguages=${getLang("en")}" }
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_SET_TRANSLATE_LANGUAGES_WITHOUT_TRANSLATIONS_EDIT_SCOPE)

    permissionTestUtil.performSetPermissions(
      ""
    ) { getLang -> "scopes=translations.view&stateChangeLanguages=${getLang("en")}" }
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_SET_STATE_CHANGE_LANGUAGES_WITHOUT_TRANSLATIONS_STATE_EDIT_SCOPE)
    permissionTestUtil.performSetPermissions(
      ""
    ) { getLang -> "scopes=screenshots.upload&viewLanguages=${getLang("en")}" }
      .andIsBadRequest
      .andHasErrorMessage(Message.CANNOT_SET_VIEW_LANGUAGES_WITHOUT_TRANSLATIONS_VIEW_SCOPE)
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
  fun `validates permissions (admin and languages)`() {
    permissionTestUtil.performSetPermissions("") { getLang ->
      "scopes=admin&translateLanguages=${getLang("en")}"
    }.andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
    permissionTestUtil.performSetPermissions("") { getLang ->
      "scopes=admin&viewLanguages=${getLang("en")}"
    }.andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
    permissionTestUtil.performSetPermissions("") { getLang ->
      "scopes=admin&stateChangeLanguages=${getLang("en")}"
    }.andIsBadRequest.andHasErrorMessage(Message.CANNOT_SET_LANGUAGE_PERMISSIONS_FOR_ADMIN_SCOPE)
  }
}
