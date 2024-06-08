/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class LanguageServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var authenticationFacade: AuthenticationFacade

  @Test
  @Transactional
  fun `remove of language removes existing language reference from import language`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    var foundImportLanguage = importService.findLanguages(testData.import).first()
    assertThat(foundImportLanguage.existingLanguage!!.id).isEqualTo(testData.english.id)
    languageService.deleteLanguage(testData.german.id)
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
      setAuthentication(testData)
      languageService.deleteLanguage(testData.germanLanguage.id)
    }

    executeInNewTransaction {
      waitForNotThrowing(timeout = 10000, pollTime = 100) {
        assertLanguageDeleted(testData)
        assertActivityCreated()
      }
    }
  }

  private fun setAuthentication(testData: TranslationsTestData) {
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        null,
        UserAccountDto.fromEntity(testData.user),
        null,
      )
  }

  private fun assertLanguageDeleted(testData: TranslationsTestData) {
    entityManager.createQuery("select 1 from Language l where l.id = :id")
      .setParameter("id", testData.germanLanguage.id)
      .resultList.assert.isEmpty()
  }

  private fun assertActivityCreated() {
    val result =
      entityManager.createQuery(
        """select ar.id, ame.modifications, ame.describingData from ActivityRevision ar 
            |join ar.modifiedEntities ame
            |where ar.type = :type
        """.trimMargin(),
      )
        .setParameter("type", ActivityType.HARD_DELETE_LANGUAGE)
        .resultList
    result.assert.hasSize(3)
  }
}
