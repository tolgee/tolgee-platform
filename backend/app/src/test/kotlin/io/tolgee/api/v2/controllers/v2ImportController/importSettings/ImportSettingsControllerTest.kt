package io.tolgee.api.v2.controllers.v2ImportController.importSettings

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.dataImport.ImportSettings
import io.tolgee.model.dataImport.ImportSettingsId
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ImportSettingsControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  @ProjectJWTAuthTestMethod
  fun `stores settings`() {
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
    performProjectAuthPut(
      "import-settings",
      mapOf(
        "overrideKeyDescriptions" to true,
        "convertPlaceholdersToIcu" to false,
      ),
    ).andIsOk.andAssertThatJson {
      node("overrideKeyDescriptions").isBoolean.isTrue
      node("convertPlaceholdersToIcu").isBoolean.isFalse
    }

    executeInNewTransaction {
      entityManager.getReference(ImportSettings::class.java, ImportSettingsId(userAccount!!.id, project.id)).apply {
        this.overrideKeyDescriptions.assert.isTrue()
        this.convertPlaceholdersToIcu.assert.isFalse()
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns settings`() {
    val testData = BaseTestData()
    testData.projectBuilder.setImportSettings {
      userAccount = testData.user
      overrideKeyDescriptions = true
      convertPlaceholdersToIcu = false
    }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }

    performProjectAuthGet(
      "import-settings",
    ).andIsOk.andAssertThatJson {
      node("overrideKeyDescriptions").isBoolean.isTrue
      node("convertPlaceholdersToIcu").isBoolean.isFalse
    }
  }
}
