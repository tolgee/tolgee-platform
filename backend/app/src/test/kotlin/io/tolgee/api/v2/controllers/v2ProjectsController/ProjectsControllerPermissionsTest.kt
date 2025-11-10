package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.PermissionTestUtil
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerPermissionsTest : ProjectAuthControllerTest("/v2/projects/") {
  private val permissionTestUtil: PermissionTestUtil by lazy { PermissionTestUtil(this, applicationContext) }

  @Test
  fun `sets user permissions`() {
    permissionTestUtil.withPermissionsTestData { project, user ->
      performAuthPut("/v2/projects/${project.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

      permissionService
        .getProjectPermissionScopesNoApiKey(project.id, user)
        .let { Assertions.assertThat(it).equalsPermissionType(ProjectPermissionType.EDIT) }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `changes from none to other`() {
    val testData = PermissionsTestData()
    val user = testData.addUserWithPermissions(type = ProjectPermissionType.NONE)
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin.self
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("users/${user.id}/set-permissions/EDIT").andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot set permission to user outside of project or organization`() {
    val testData = PermissionsTestData()
    val user =
      testData.root
        .addUserAccount {
          username = "pepa@seznam.cz"
        }.self
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin.self
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("users/${user.id}/set-permissions/EDIT")
      .andIsBadRequest
      .andHasErrorMessage(Message.USER_HAS_NO_PROJECT_ACCESS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sets user permissions to organization base`() {
    val testData = PermissionsTestData()
    val me =
      testData.addUserWithPermissions(
        type = ProjectPermissionType.EDIT,
        organizationBaseScopes = listOf(Scope.KEYS_VIEW),
      )
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin.self
    this.projectSupplier = { testData.projectBuilder.self }

    permissionService
      .getProjectPermissionData(
        testData.projectBuilder.self.id,
        me.id,
      ).directPermissions.assert.isNotNull
    performProjectAuthPut("users/${me.id}/set-by-organization").andIsOk
    val permissionData = permissionService.getProjectPermissionData(testData.projectBuilder.self.id, me.id)
    permissionData.directPermissions.assert.isNull()
    permissionData.organizationRole.assert.isNotNull
  }

  @Test
  fun `sets user's permissions with languages`() {
    permissionTestUtil.checkSetPermissionsWithLanguages("TRANSLATE", { getLang ->
      "languages=${getLang("en")}&" +
        "languages=${getLang("de")}"
    }) { data, getLang ->
      Assertions.assertThat(data.computedPermissions.translateLanguageIds).contains(getLang("en"))
      Assertions.assertThat(data.computedPermissions.translateLanguageIds).contains(getLang("de"))
      // can view all languages
      Assertions.assertThat(data.computedPermissions.viewLanguageIds).isEmpty()
      Assertions.assertThat(data.computedPermissions.stateChangeLanguageIds).isEmpty()
    }
  }

  @Test
  fun `cannot save stateChangeLanguages when translate`() {
    permissionTestUtil
      .performSetPermissions("TRANSLATE") { getLang ->
        "stateChangeLanguages=${getLang("de")}"
      }.andIsBadRequest
  }

  @Test
  fun `cannot save viewLanguages when none`() {
    permissionTestUtil
      .performSetPermissions("NONE") { getLang ->
        "viewLanguages=${getLang("de")}"
      }.andIsBadRequest
  }

  @Test
  fun `cannot save stateChangeLanguages when view`() {
    permissionTestUtil
      .performSetPermissions("VIEW") { getLang ->
        "stateChangeLanguages=${getLang("de")}"
      }.andIsBadRequest
  }

  @Test
  fun `cannot save translationEditLangueges when view`() {
    permissionTestUtil
      .performSetPermissions("VIEW") { getLang ->
        "translateLanguages=${getLang("de")}"
      }.andIsBadRequest
  }
}
