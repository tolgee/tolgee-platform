/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.development.testDataBuilder.data.PromptTestData
import io.tolgee.development.testDataBuilder.data.TranslationCommentsTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
  properties = [
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
  ],
)
class LanguageServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var authenticationFacade: AuthenticationFacade

  @Autowired
  private lateinit var sessionFactory: SessionFactory

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
  fun `remove of language removes existing translation conflict references from import translations`() {
    val testData = ImportTestData()
    testDataService.saveTestData(testData.root)

    assertThat(importService.findTranslation(testData.translationWithConflict.id)!!.conflict).isNotNull()
    languageService.deleteLanguage(testData.english.id)
    entityManager.flush()
    entityManager.clear()
    assertThat(importService.findTranslation(testData.translationWithConflict.id)!!.conflict).isNull()
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
  @Transactional
  fun `deletes language with ai results`() {
    val testData = PromptTestData()
    testDataService.saveTestData(testData.root)
    languageService.hardDeleteLanguage(testData.czech.self.id)
  }

  @Test
  fun `hard deletes language`() {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)

    executeInNewTransaction {
      setAuthentication(testData.user)
      languageService.deleteLanguage(testData.germanLanguage.id)
    }

    executeInNewTransaction {
      waitForNotThrowing(timeout = 10000, pollTime = 100) {
        assertLanguageDeleted(testData.germanLanguage)
        assertActivityCreated()
      }
    }
  }

  @Test
  fun `hard deletes language with conflicts`() {
    val testData = ImportTestData()
    testData.useCzechBaseLanguage()
    testDataService.saveTestData(testData.root)

    executeInNewTransaction {
      setAuthentication(testData.userAccount)
      languageService.deleteLanguage(testData.english.id)
    }

    executeInNewTransaction {
      waitForNotThrowing(timeout = 10000, pollTime = 100) {
        assertLanguageDeleted(testData.english)
      }
    }
  }

  @Test
  fun `hard deletes language without n+1s`() {
    val testData = TranslationsTestData()
    testData.generateLotOfData(100)
    testDataService.saveTestData(testData.root)

    sessionFactory.statistics.clear()
    executeInNewTransaction {
      setAuthentication(testData.user)
      languageService.hardDeleteLanguage(testData.germanLanguage.id)
    }
    val count = sessionFactory.statistics.prepareStatementCount
    count.assert.isLessThan(20)

    executeInNewTransaction {
      assertLanguageDeleted(testData.germanLanguage)
    }
  }

  private fun setAuthentication(user: UserAccount) {
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = null,
        deviceId = null,
        userAccount = UserAccountDto.fromEntity(user),
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }

  private fun assertLanguageDeleted(language: Language) {
    entityManager
      .createQuery("select 1 from Language l where l.id = :id")
      .setParameter("id", language.id)
      .resultList.assert
      .isEmpty()
  }

  private fun assertActivityCreated() {
    val result =
      entityManager
        .createQuery(
          """select ar.id, ame.modifications, ame.describingData from ActivityRevision ar 
           join ar.modifiedEntities ame
           where ar.type = :type
        """,
        ).setParameter("type", ActivityType.HARD_DELETE_LANGUAGE)
        .resultList
    result.assert.hasSize(3)
  }
}
