package io.tolgee.api.v2.controllers

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
}
