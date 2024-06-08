/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.assert
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
    languageService.hardDeleteLanguage(testData.german.id)
    entityManager.flush()
    entityManager.clear()
    foundImportLanguage = importService.findLanguages(testData.import).find { it.name == "de" }!!
    assertThat(foundImportLanguage.existingLanguage).isEqualTo(null)
  }

  @Test
  @Transactional
  fun `deletes language with MT Service Config`() {
    val testData = MtSettingsTestData()
    testDataService.saveTestData(testData.root)
    entityManager.flush()
    languageService.hardDeleteLanguage(testData.germanLanguage.id)
    languageService.findEntity(testData.germanLanguage.id).assert.isNull()
  }

  @Test
  @Transactional
  fun `deletes language with Comments`() {
    val testData = TranslationCommentsTestData()
    testDataService.saveTestData(testData.root)
    entityManager.flush()
    languageService.hardDeleteLanguage(testData.englishLanguage.id)
    languageService.findEntity(testData.englishLanguage.id).assert.isNull()
  }

  @Test
  fun `hard deletes language`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    executeInNewTransaction {
      languageService.deleteLanguage(testData.germanLanguage.id)
    }

    executeInNewTransaction {
      waitForNotThrowing(timeout = 10000, pollTime = 100) {
        entityManager.createQuery("select 1 from Language l where l.id = :id")
          .setParameter("id", testData.germanLanguage.id)
          .resultList.assert.isEmpty()
      }
    }
  }
}
