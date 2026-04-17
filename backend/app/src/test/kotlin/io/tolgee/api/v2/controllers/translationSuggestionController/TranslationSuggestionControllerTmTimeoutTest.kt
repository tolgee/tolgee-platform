package io.tolgee.api.v2.controllers.translationSuggestionController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.translation.TranslationMemoryService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.QueryTimeoutException
import org.springframework.test.context.bean.override.mockito.MockitoBean

class TranslationSuggestionControllerTmTimeoutTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionTestData

  @Autowired
  @MockitoBean
  lateinit var translationMemoryService: TranslationMemoryService

  @BeforeEach
  fun setup() {
    testData = SuggestionTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns empty page when TM query times out with baseText`() {
    whenever(translationMemoryService.getSuggestions(any<String>(), any(), anyOrNull(), any(), any()))
      .thenThrow(QueryTimeoutException("TM query timed out"))

    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(baseText = "This is beautiful", targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns empty page when TM query times out with keyId`() {
    whenever(translationMemoryService.getSuggestions(any<io.tolgee.model.key.Key>(), any(), any()))
      .thenThrow(QueryTimeoutException("TM query timed out"))

    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(keyId = testData.thisIsBeautifulKey.id, targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }
}
