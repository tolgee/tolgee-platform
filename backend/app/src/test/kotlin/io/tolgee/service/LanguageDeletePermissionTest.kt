/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class LanguageDeletePermissionTest : AbstractSpringTest() {
  lateinit var testData: LanguagePermissionsTestData

  @BeforeEach
  fun beforeEach() {
    testData = LanguagePermissionsTestData()
    testData.project.baseLanguage = testData.germanLanguage
    testDataService.saveTestData(testData.root)
    entityManager.flush()

    languageService.deleteLanguage(testData.englishLanguage.id)
    languageService.findEntity(testData.englishLanguage.id).assert.isNull()
  }

  @Test
  @Transactional
  fun `lowers permissions for translate langs`() {
    checkUser(testData.translateEnOnlyUser) {
      assertThat(computedPermissions.scopes)
        .containsAll(ProjectPermissionType.VIEW.availableScopes.toList())
    }

    checkUser(testData.translateAllUser) {
      assertThat(computedPermissions.scopes)
        .containsAll(ProjectPermissionType.TRANSLATE.availableScopes.toList())
    }

    checkUser(testData.translateAllExplicitUser) {
      assertThat(computedPermissions.scopes)
        .containsAll(ProjectPermissionType.TRANSLATE.availableScopes.toList())
      assertThat(computedPermissions.translateLanguageIds).hasSize(1)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for all`() {
    checkUser(testData.reviewUser) {
      assertThat(computedPermissions.type).isEqualTo(ProjectPermissionType.NONE)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for view & translate`() {
    checkUser(testData.translateUser) {
      assertThat(computedPermissions.type).isEqualTo(ProjectPermissionType.NONE)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for view`() {
    checkUser(testData.viewEnOnlyUser) {
      assertThat(computedPermissions.type).isEqualTo(ProjectPermissionType.NONE)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for translate view (granular)`() {
    checkUser(testData.viewScopeUser) {
      assertThat(computedPermissions.scopes).containsExactly(Scope.KEYS_VIEW)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for translate edit (granular)`() {
    checkUser(testData.editScopeUser) {
      assertThat(computedPermissions.scopes).containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for translate state change (granular)`() {
    checkUser(testData.stateChangeScopeUser) {
      assertThat(computedPermissions.scopes)
        .containsExactlyInAnyOrder(Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_EDIT, Scope.KEYS_VIEW)
    }
  }

  @Test
  @Transactional
  fun `lowers permissions for translate state change (only en for all operations) (granular)`() {
    checkUser(testData.stateChangeScopeUserEnForAll) {
      assertThat(computedPermissions.scopes).containsExactly(Scope.KEYS_VIEW)
    }
  }

  private fun checkUser(
    userAccount: UserAccount,
    checkFn: ProjectPermissionData.() -> Unit,
  ) {
    val data = permissionService.getProjectPermissionData(testData.project.id, userAccount.id)
    checkFn(data)
  }
}
