package io.tolgee.ee.component.qa

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.events.OnTranslationsSet
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
class QaCheckTranslationListenerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var listener: QaCheckTranslationListener

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  lateinit var testData: BaseTestData
  lateinit var testKey: Key
  lateinit var frTranslation: Translation

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testData.projectBuilder.build {
      addFrench()
      testKey =
        addKey {
          name = "test-key"
        }.build {
          addTranslation("en", "Hello world.")
          frTranslation =
            addTranslation("fr", "bonjour monde").self
        }.self
    }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
  }

  private fun refetchEntities() {
    testKey = entityManager.find(Key::class.java, testKey.id)
    frTranslation = entityManager.find(Translation::class.java, frTranslation.id)
  }

  @Test
  @Transactional
  fun `creates QA issues when translation has problems`() {
    refetchEntities()

    val event =
      OnTranslationsSet(
        source = this,
        key = testKey,
        oldValues = mapOf("fr" to null),
        translations = listOf(frTranslation),
      )

    listener.processTranslationsSet(event)
    entityManager.flush()

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issues).isNotEmpty
    assertThat(issues.map { it.type.name }).contains("CHARACTER_CASE_MISMATCH", "PUNCTUATION_MISMATCH")
  }

  @Test
  @Transactional
  fun `creates no issues when translation is clean`() {
    refetchEntities()

    frTranslation.text = "Bonjour monde."
    entityManager.flush()

    val event =
      OnTranslationsSet(
        source = this,
        key = testKey,
        oldValues = mapOf("fr" to "bonjour monde"),
        translations = listOf(frTranslation),
      )

    listener.processTranslationsSet(event)
    entityManager.flush()

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issues).isEmpty()
  }

  @Test
  @Transactional
  fun `replaces existing issues on re-check`() {
    refetchEntities()

    val event =
      OnTranslationsSet(
        source = this,
        key = testKey,
        oldValues = mapOf("fr" to null),
        translations = listOf(frTranslation),
      )

    listener.processTranslationsSet(event)
    entityManager.flush()
    val issuesBefore = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issuesBefore).isNotEmpty

    // Update translation to be clean and re-check
    frTranslation.text = "Bonjour monde."
    entityManager.flush()

    val event2 =
      OnTranslationsSet(
        source = this,
        key = testKey,
        oldValues = mapOf("fr" to "bonjour monde"),
        translations = listOf(frTranslation),
      )
    listener.processTranslationsSet(event2)
    entityManager.flush()

    val issuesAfter = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issuesAfter).isEmpty()
  }
}
