package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportNamespacesTestData
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class V2ImportControllerManipulationTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  fun `it deletes import`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id

    loginAsUser(user.username)

    performAuthDelete("/v2/projects/$projectId/import", null).andIsOk
    assertThat(importService.find(projectId, user.id)).isNull()
  }

  @Test
  fun `it deletes import language`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/result/languages/${testData.importEnglish.id}"
    performAuthDelete(path, null).andIsOk
    assertThat(importService.findLanguage(testData.importEnglish.id)).isNull()
  }

  @Test
  fun `it resolves import translation conflict (override)`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path =
      "/v2/projects/$projectId/import/result/languages/${testData.importEnglish.id}" +
        "/translations/${testData.translationWithConflict.id}/resolve/set-override"
    performAuthPut(path, null).andIsOk
    val translation = importService.findTranslation(testData.translationWithConflict.id)
    assertThat(translation?.resolved).isTrue
    assertThat(translation?.override).isTrue
  }

  @Test
  fun `it resolves import translation conflict (keep)`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path =
      "/v2/projects/$projectId/import/result/languages/${testData.importEnglish.id}" +
        "/translations/${testData.translationWithConflict.id}/resolve/set-keep-existing"
    performAuthPut(path, null).andIsOk
    val translation = importService.findTranslation(testData.translationWithConflict.id)
    assertThat(translation?.resolved).isTrue
    assertThat(translation?.override).isFalse
  }

  @Test
  fun `it resolves all language translation conflicts (override)`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/result/languages/${testData.importEnglish.id}/resolve-all/set-override"
    performAuthPut(path, null).andIsOk
    val translation = importService.findTranslation(testData.translationWithConflict.id)
    assertThat(translation?.resolved).isTrue
    assertThat(translation?.override).isTrue
  }

  @Test
  fun `it resolves all language translation conflicts (keep)`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path =
      "/v2/projects/$projectId/import/result/languages/" +
        "${testData.importEnglish.id}/resolve-all/set-keep-existing"
    performAuthPut(path, null).andIsOk
    val translation = importService.findTranslation(testData.translationWithConflict.id)
    assertThat(translation?.resolved).isTrue
    assertThat(translation?.override).isFalse
  }

  @Test
  fun `it selects language`() {
    val testData = ImportTestData()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path =
      "/v2/projects/$projectId/import/result/languages/${testData.importFrench.id}/" +
        "select-existing/${testData.french.id}"
    performAuthPut(path, null).andIsOk
    assertThat(importService.findLanguage(testData.importFrench.id)?.existingLanguage)
      .isEqualTo(testData.french)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it selects namespace, resets conflicts`() {
    val testData = ImportNamespacesTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.userAccount
    val path = "import/result/files/${testData.defaultNsFile.id}/select-namespace"
    performProjectAuthPut(path, mapOf("namespace" to "new-ns")).andIsOk

    executeInNewTransaction {
      val file =
        importService.findFile(projectId = project.id, authorId = userAccount!!.id, testData.defaultNsFile.id)!!
      assertThat(file.namespace)
        .isEqualTo("new-ns")
      val importLanguage =
        file.languages
          .find { it.name == "de" }!!
      importLanguage.translations
        .any { it.conflict != null }
        .assert
        .isEqualTo(false)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns all namespaces`() {
    val testData = ImportNamespacesTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.userAccount
    performProjectAuthGet("import/all-namespaces").andIsOk.andAssertThatJson {
      node("_embedded.namespaces") {
        node("[0].name").isEqualTo("existing-namespace")
        node("[1].name").isEqualTo("existing-namespace2")
        node("[2].name").isEqualTo("homepage")
      }
    }
  }

  @Test
  fun `it selects same language for different namespaces`() {
    val testData = ImportTestData()
    // assign the existing french to the import french
    testData.importFrench.existingLanguage = testData.french
    val nsData = testData.addFilesWithNamespaces()
    nsData.importFrenchInNs.existingLanguage = null
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    // try to assign with another french but in different namespace
    val path =
      "/v2/projects/$projectId/import/result/languages/${nsData.importFrenchInNs.id}/" +
        "select-existing/${testData.french.id}"
    performAuthPut(path, null).andIsOk
    assertThat(importService.findLanguage(testData.importFrench.id)?.existingLanguage)
      .isEqualTo(testData.french)
  }

  @Test
  fun `conflicts are refreshed when changing namespace`() {
    val testData = ImportTestData()
    // assign the existing french to the import french
    testData.importFrench.existingLanguage = testData.french
    val nsData = testData.addFilesWithNamespaces()
    nsData.importFrenchInNs.existingLanguage = null
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    // try to assign with another french but in different namespace
    val path =
      "/v2/projects/$projectId/import/result/languages/${nsData.importFrenchInNs.id}/" +
        "select-existing/${testData.french.id}"
    performAuthPut(path, null).andIsOk
    assertThat(importService.findLanguage(testData.importFrench.id)?.existingLanguage)
      .isEqualTo(testData.french)
  }

  @Test
  fun `it resets selected language`() {
    val testData = ImportTestData()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user =
      testData.root.data.userAccounts[0]
        .self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/result/languages/${testData.importEnglish.id}/reset-existing"
    performAuthPut(path, null).andIsOk
    assertThat(importService.findLanguage(testData.importEnglish.id)?.existingLanguage)
      .isNull()
  }
}
