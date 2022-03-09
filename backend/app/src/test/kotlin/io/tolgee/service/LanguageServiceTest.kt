/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.ImportTestData
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.model.Permission
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class LanguageServiceTest : AbstractSpringTest() {
  @Test
  @Transactional
  fun `remove of language removes existing language reference from import language`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    var foundImportLanguage = importService.findLanguages(testData.import).first()
    assertThat(foundImportLanguage.existingLanguage!!.id).isEqualTo(testData.english.id)
    languageService.deleteLanguage(testData.english.id)
    entityManager.flush()
    entityManager.clear()
    foundImportLanguage = importService.findLanguages(testData.import).first()
    assertThat(foundImportLanguage.existingLanguage).isEqualTo(null)
  }

  @Test
  @Transactional
  fun `deletes language with MT Service Config`() {
    val testData = MtSettingsTestData()
    testDataService.saveTestData(testData.root)
    entityManager.flush()
    languageService.deleteLanguage(testData.germanLanguage.id)
  }

  @Test
  @Transactional
  fun `sets view permission when deleted only permitted language`() {
    val testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)
    entityManager.flush()

    languageService.deleteLanguage(testData.englishLanguage.id)

    val enUserData = permissionService.getProjectPermissionData(testData.project.id, testData.enOnlyUser.id)
    assertThat(enUserData.computedPermissions.type).isEqualTo(Permission.ProjectPermissionType.VIEW)

    val allLangUserData = permissionService.getProjectPermissionData(testData.project.id, testData.allLangUser.id)
    assertThat(allLangUserData.computedPermissions.type).isEqualTo(Permission.ProjectPermissionType.TRANSLATE)

    val explicitBothLangsUserData = permissionService.getProjectPermissionData(
      testData.project.id,
      testData.bothLangsExplicitUser.id
    )
    assertThat(explicitBothLangsUserData.computedPermissions.type).isEqualTo(Permission.ProjectPermissionType.TRANSLATE)
    assertThat(explicitBothLangsUserData.computedPermissions.languageIds).hasSize(1)
  }
}
