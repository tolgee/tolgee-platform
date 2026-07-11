package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportPluralizationTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class V2ImportControllerPluralizationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ImportPluralizationTestData

  @Test
  @ProjectJWTAuthTestMethod
  fun `it correctly migrates data`() {
    saveTestDataAndApplyImport()
    // new value is migrated
    getTranslation("cs", "existing plural key")
      .text.assert
      .isEqualTo("{count, plural,\nother {No plural}\n}")

    // migrates old existing values
    getTranslation("en", "existing non plural key")
      .text.assert
      .isEqualTo("{count, plural,\nother {I am not a plural!}\n}")

    // keeps non-plurals
    getTranslation("en", "existing non plural key 2")
      .text.assert
      .isEqualTo("I am not a plural!")
    getTranslation("cs", "existing non plural key 2")
      .text.assert
      .isEqualTo("Nejsem plurál!")
  }

  private fun saveTestDataAndApplyImport() {
    testData = ImportPluralizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userAccount
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("import/apply").andIsOk
  }

  private fun getTranslation(
    language: String,
    key: String,
  ): Translation {
    return entityManager
      .createQuery(
        """
      from Translation t
      join t.key k
      join t.language l
      where k.name = :key and l.tag = :language and k.project.id = :projectId
    """,
        Translation::class.java,
      ).setParameter("key", key)
      .setParameter("projectId", testData.projectBuilder.self.id)
      .setParameter("language", language)
      .singleResult
  }
}
