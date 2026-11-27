package io.tolgee.service.translationMemory

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.service.translation.AbstractTranslationMemoryService.Companion.MAX_SUGGESTION_SOURCE_LENGTH
import io.tolgee.service.translation.TranslationMemoryService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TranslationMemoryLongTextTest : AbstractSpringTest() {
  @Autowired
  lateinit var translationMemoryService: TranslationMemoryService

  lateinit var testData: TranslationMemoryTestData

  private val longText = "This paragraph is part of a very long help text. ".repeat(30)

  @BeforeEach
  fun setup() {
    assertThat(longText.length).isGreaterThan(MAX_SUGGESTION_SOURCE_LENGTH)
    testData = TranslationMemoryTestData()
    testData.addLongSourceKey(longText)
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `suggestions are skipped for source text over the length limit`() {
    val project = testData.projectWithTm

    val page =
      translationMemoryService.getSuggestions(
        baseTranslationText = longText,
        isPlural = false,
        keyId = null,
        projectId = project.id,
        organizationId = project.organizationOwner.id,
        targetLanguageTag = "de",
        pageable = PageRequest.of(0, 10),
      )

    assertThat(page.content).isEmpty()
  }

  @Test
  fun `MT examples are skipped for source text over the length limit`() {
    val project = testData.projectWithTm

    val results =
      translationMemoryService.getSuggestionsList(
        baseTranslationText = longText,
        isPlural = false,
        keyId = null,
        projectId = project.id,
        organizationId = project.organizationOwner.id,
        targetLanguageTag = "de",
        limit = 5,
      )

    assertThat(results).isEmpty()
  }
}
