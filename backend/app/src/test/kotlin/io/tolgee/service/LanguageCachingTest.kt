/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.model.Language
import io.tolgee.repository.LanguageRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class LanguageCachingTest : AbstractSpringTest() {
  @MockitoSpyBean
  @Autowired
  private lateinit var languageRepository: LanguageRepository

  private lateinit var testData: BaseTestData

  private lateinit var germanLanguage: Language

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    germanLanguage = testData.projectBuilder.addGerman().self
    testDataService.saveTestData(testData.root)
    clearCaches()
    Mockito.reset(languageRepository)
  }

  @Test
  fun `it caches languages`() {
    languageService.getProjectLanguages(testData.project.id)
    assertRepositoryInvocationOnce()
    languageService.getProjectLanguages(testData.project.id)
    assertRepositoryInvocationOnce()
  }

  private fun assertRepositoryInvocationOnce() {
    verify(languageRepository, times(1)).findAllDtosByProjectId(eq(testData.project.id))
  }

  @Test
  fun `it evict cache on create`() {
    populateCache()
    executeInNewTransaction {
      languageService.createLanguage(
        getLanguageDto(),
        project = testData.project,
      )
    }
    assertCacheEvicted()
  }

  private fun getLanguageDto() =
    LanguageRequest(
      name = "test",
      tag = "test",
      originalName = "test",
      flagEmoji = "test",
    )

  @Test
  fun `it evict cache on delete`() {
    populateCache()
    executeInNewTransaction {
      languageService.deleteLanguage(languageService.getEntity(germanLanguage.id))
    }
    assertCacheEvicted()
  }

  @Test
  fun `it evict cache on base language changge`() {
    populateCache()
    executeInNewTransaction {
      projectService.editProject(
        testData.project.id,
        EditProjectRequest(
          name = "test",
          baseLanguageId = germanLanguage.id,
        ),
      )
    }
    assertCacheEvicted()
  }

  @Test
  fun `it evict cache on update`() {
    populateCache()
    languageService.editLanguage(
      testData.englishLanguage.id,
      testData.project.id,
      getLanguageDto(),
    )
    assertCacheEvicted()
  }

  private fun populateCache() {
    languageService.getProjectLanguages(testData.project.id)
    assertCachePopulated()
  }

  private fun assertCachePopulated() {
    cacheManager
      .getCache(Caches.LANGUAGES)!!
      .get(testData.project.id)
      ?.get()
      .assert.isNotNull
  }

  private fun assertCacheEvicted() {
    cacheManager
      .getCache(Caches.LANGUAGES)!!
      .get(testData.project.id)
      ?.get()
      .assert
      .isNull()
  }
}
