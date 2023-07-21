package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class V2ImportControllerApplicationTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  fun `it applies the import`() {
    val testData = ImportTestData()
    testData.setAllResolved()
    testData.setAllOverride()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply"
    performAuthPut(path, null).andIsOk
    this.importService.find(projectId, user.id).let {
      assertThat(it).isNull()
    }
  }

  @Test
  fun `it applies the import with force override`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply?forceMode=OVERRIDE"
    performAuthPut(path, null).andIsOk
    this.importService.find(projectId, user.id).let {
      assertThat(it).isNull()
    }
  }

  @Test
  fun `it applies the import with force keep`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply?forceMode=KEEP"
    performAuthPut(path, null).andIsOk
  }

  @Test
  fun `it imports empty keys`() {
    val testData = ImportTestData()
    testData.addEmptyKey()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply?forceMode=KEEP"
    performAuthPut(path, null).andIsOk

    executeInNewTransaction {
      projectService.get(testData.project.id).keys.find { it.name == "empty key" }.assert.isNotNull
    }
  }

  @Test
  fun `it checks permissions`() {
    val testData = ImportTestData()

    val user = testData.useTranslateOnlyUser()

    testDataService.saveTestData(testData.root)
    val projectId = testData.project.id
    loginAsUser(user.username)

    val path = "/v2/projects/$projectId/import/apply?forceMode=OVERRIDE"
    performAuthPut(path, null).andIsForbidden.andAssertThatJson {
      node("params") {
        node("[0]").isEqualTo(""""keys.create"""")
      }
    }
  }

  @Test
  fun `it checks language permissions`() {
    val testData = ImportTestData()
    testData.importBuilder.data.importFiles[0].data.importKeys.removeIf { it.self == testData.newLongKey }
    val resolveFrench = testData.addFrenchTranslations()
    resolveFrench()

    val user = testData.useTranslateOnlyUser()

    testDataService.saveTestData(testData.root)
    val projectId = testData.project.id
    loginAsUser(user.username)

    val path = "/v2/projects/$projectId/import/apply?forceMode=OVERRIDE"
    performAuthPut(path, null).andIsForbidden.andAssertThatJson {
      node("code").isEqualTo("language_not_permitted")
      node("params[0]") {
        isArray
        node("[0]").isValidId
      }
    }
  }

  @Test
  fun `it checks permissions (view only)`() {
    val testData = ImportTestData()
    testData.importBuilder.data.importFiles[0].data.importKeys.removeIf { it.self == testData.newLongKey }
    val resolveFrench = testData.addFrenchTranslations()
    resolveFrench()

    val user = testData.useViewEnOnlyUser()

    testDataService.saveTestData(testData.root)
    val projectId = testData.project.id
    loginAsUser(user.username)

    val path = "/v2/projects/$projectId/import/apply?forceMode=OVERRIDE"
    performAuthPut(path, null).andIsForbidden
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `it checks permissions with API key (view only)`() {
    val testData = ImportTestData()
    testData.importBuilder.data.importFiles[0].data.importKeys.removeIf { it.self == testData.newLongKey }
    val resolveFrench = testData.addFrenchTranslations()
    resolveFrench()

    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.userAccount

    val path = "import/apply?forceMode=OVERRIDE"
    performProjectAuthPut(path, null).andIsForbidden
  }

  @Test
  fun `it sets outdated on update`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)
    val user = testData.root.data.userAccounts[0].self
    val projectId = testData.project.id
    loginAsUser(user.username)
    val path = "/v2/projects/$projectId/import/apply?forceMode=OVERRIDE"
    performAuthPut(path, null).andIsOk

    executeInNewTransaction {
      val key = projectService.get(testData.project.id)
        .keys.find { it.name == "what a nice key" }!!

      val untouched = key.translations.find { it.language == testData.french }!!
      untouched.outdated.assert.isEqualTo(true)
      untouched.mtProvider.assert.isEqualTo(MtServiceType.GOOGLE)
      untouched.auto.assert.isEqualTo(true)

      val touched = key.translations.find { it.language == testData.english }!!
      touched.outdated.assert.isEqualTo(false)
      touched.mtProvider.assert.isEqualTo(null)
      touched.auto.assert.isEqualTo(false)
    }
  }
}
