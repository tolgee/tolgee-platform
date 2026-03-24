package io.tolgee.ee.service.qa

import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
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
class QaBaseChangeRecheckTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchServiceImpl

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  @Autowired
  lateinit var qa: QaTestUtil

  lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
  }

  private fun refetchEntities() {
    testData.testKey = entityManager.find(Key::class.java, testData.testKey.id)
    testData.enTranslation = entityManager.find(Translation::class.java, testData.enTranslation.id)
    testData.frTranslation = entityManager.find(Translation::class.java, testData.frTranslation.id)
  }

  @Test
  @Transactional
  fun `sibling QA issues change when base text changes`() {
    refetchEntities()

    // Initial check: FR "bonjour monde" vs EN "Hello world." has case + punctuation issues
    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()
    val issuesBefore = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issuesBefore.map { it.type.name }).contains("CHARACTER_CASE_MISMATCH", "PUNCTUATION_MISMATCH")

    // Change base text to match FR style: lowercase, no period
    testData.enTranslation.text = "bonjour monde"
    entityManager.flush()

    // Recheck FR against new base — issues should be gone
    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.testKey.id,
      testData.frTranslation.language.id,
    )
    entityManager.flush()
    val issuesAfter = qaIssueRepository.findAllByTranslationId(testData.frTranslation.id)
    assertThat(issuesAfter.map { it.type.name }).doesNotContain("CHARACTER_CASE_MISMATCH", "PUNCTUATION_MISMATCH")
  }

  @Test
  @Transactional
  fun `getSiblingIdsForBaseLanguageChanges returns sibling IDs`() {
    refetchEntities()

    val baseLanguage = languageService.getProjectBaseLanguage(testData.project.id)
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = listOf(testData.enTranslation.id),
        baseLanguageId = baseLanguage.id,
      )

    assertThat(siblingIds).containsExactly(testData.frTranslation.id)
  }

  @Test
  @Transactional
  fun `getSiblingIdsForBaseLanguageChanges returns empty when no base translations in input`() {
    refetchEntities()

    val baseLanguage = languageService.getProjectBaseLanguage(testData.project.id)
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = listOf(testData.frTranslation.id),
        baseLanguageId = baseLanguage.id,
      )

    assertThat(siblingIds).isEmpty()
  }

  @Test
  @Transactional
  fun `getSiblingIdsForBaseLanguageChanges excludes translations already in input`() {
    refetchEntities()

    val baseLanguage = languageService.getProjectBaseLanguage(testData.project.id)
    // Include both EN (base) and FR in input — FR should not appear in siblings
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = listOf(testData.enTranslation.id, testData.frTranslation.id),
        baseLanguageId = baseLanguage.id,
      )

    assertThat(siblingIds).isEmpty()
  }

  @Test
  @Transactional
  fun `getSiblingIdsForBaseLanguageChanges includes empty translations`() {
    refetchEntities()

    // Set FR translation to null (empty)
    testData.frTranslation.text = null
    entityManager.flush()

    val baseLanguage = languageService.getProjectBaseLanguage(testData.project.id)
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = listOf(testData.enTranslation.id),
        baseLanguageId = baseLanguage.id,
      )

    // Empty translations should still be included
    assertThat(siblingIds).containsExactly(testData.frTranslation.id)
  }
}
