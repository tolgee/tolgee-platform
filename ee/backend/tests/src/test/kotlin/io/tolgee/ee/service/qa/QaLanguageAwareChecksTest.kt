package io.tolgee.ee.service.qa

import io.tolgee.development.testDataBuilder.data.QaLanguageAwareChecksTestData
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

/**
 * Runs real-world translations (taken from https://github.com/tolgee/tolgee-platform/issues/4080)
 * through the whole batch QA pipeline, so plural parsing, language tags and persistence are covered.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QaLanguageAwareChecksTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchServiceImpl

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  private lateinit var testData: QaLanguageAwareChecksTestData

  @BeforeEach
  fun setup() {
    testData = QaLanguageAwareChecksTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
  }

  @Test
  fun `case check ignores strings starting with the plural number`() {
    checkAndGetIssues("n-words-highlighted", "pl").assert.isEmpty()
    checkAndGetIssues("n-words-highlighted", "fr").assert.isEmpty()
  }

  @Test
  fun `case check accepts capitalized German nouns but still reports lowercase labels`() {
    checkAndGetIssues("rows", "de").assert.isEmpty()
    checkAndGetIssues("show", "de").assert.containsExactly("CHARACTER_CASE_MISMATCH:QA_CASE_CAPITALIZE")
  }

  @Test
  fun `case check ignores brand names, code literals and abbreviations at the start`() {
    checkAndGetIssues("allow-uploads", "tr").assert.isEmpty()
    checkAndGetIssues("index-download", "fr").assert.isEmpty()
    checkAndGetIssues("placeholder", "nl").assert.isEmpty()
  }

  @Test
  fun `case check is skipped for scripts without letter case`() {
    listOf("ja", "ko", "ar", "he").forEach { checkAndGetIssues("enter-email", it).assert.isEmpty() }
  }

  @Test
  fun `case check still reports a capitalized mid-sentence operator`() {
    checkAndGetIssues("is-after", "sv").assert.containsExactly("CHARACTER_CASE_MISMATCH:QA_CASE_LOWERCASE")
  }

  @Test
  fun `placeholder check accepts spelled-out numbers in Arabic and Hebrew single-number forms`() {
    checkAndGetIssues("documents-count", "ar").assert.isEmpty()
    checkAndGetIssues("documents-count", "he").assert.isEmpty()
  }

  @Test
  fun `placeholder check still reports Arabic few and many forms with a hard-coded number`() {
    checkAndGetIssues("documents-count-broken", "ar").assert.containsExactlyInAnyOrder(
      "INCONSISTENT_PLACEHOLDERS:QA_PLACEHOLDERS_MISSING:few",
      "INCONSISTENT_PLACEHOLDERS:QA_PLACEHOLDERS_MISSING:many",
    )
  }

  @Test
  fun `placeholder check treats hash and the plural argument as the same number`() {
    checkAndGetIssues("pending-edits", "nl").assert.isEmpty()
    checkAndGetIssues("selected-items", "nl").assert.isEmpty()
    checkAndGetIssues("collections", "sv").assert.isEmpty()
  }

  @Test
  fun `placeholder check still reports a count dropped from a plural form`() {
    checkAndGetIssues("all-files", "es").assert.containsExactlyInAnyOrder(
      "INCONSISTENT_PLACEHOLDERS:QA_PLACEHOLDERS_MISSING:many",
      "INCONSISTENT_PLACEHOLDERS:QA_PLACEHOLDERS_MISSING:other",
    )
  }

  @Test
  fun `punctuation check accepts CJK and Arabic punctuation`() {
    checkAndGetIssues("not-a-member", "ja").assert.isEmpty()
    checkAndGetIssues("are-you-sure", "ar").assert.isEmpty()
    checkAndGetIssues("are-you-sure", "ja").assert.isEmpty()
    checkAndGetIssues("are-you-sure", "el").assert.isEmpty()
  }

  @Test
  fun `punctuation check still reports an added full stop`() {
    checkAndGetIssues("saved", "fr").assert.containsExactly("PUNCTUATION_MISMATCH:QA_PUNCTUATION_REMOVE")
    checkAndGetIssues("saved", "ja").assert.containsExactly("PUNCTUATION_MISMATCH:QA_PUNCTUATION_REMOVE")
  }

  @Test
  fun `brackets check accepts full-width brackets`() {
    checkAndGetIssues("toggle-department", "ja").assert.isEmpty()
  }

  @Test
  fun `brackets check accepts inclusive forms and Korean particle pairs`() {
    checkAndGetIssues("partner", "fr").assert.isEmpty()
    checkAndGetIssues("user-updated-field", "ko").assert.isEmpty()
  }

  @Test
  fun `brackets check accepts a Latin gloss added to a non-Latin-script translation`() {
    checkAndGetIssues("nda-guidelines", "ar").assert.isEmpty()
    checkAndGetIssues("nda", "zh-Hans").assert.isEmpty()
  }

  @Test
  fun `brackets check keeps short parentheticals in scripts written without spaces`() {
    checkAndGetIssues("settings-optional", "zh-Hans").assert.isEmpty()
    checkAndGetIssues("settings-optional", "ko").assert.isEmpty()
  }

  @Test
  fun `brackets check still reports a Latin gloss added to a Latin-script translation`() {
    checkAndGetIssues("nda", "fr").assert.containsExactly(
      "BRACKETS_MISMATCH:QA_BRACKETS_EXTRA",
      "BRACKETS_MISMATCH:QA_BRACKETS_EXTRA",
    )
  }

  @Test
  fun `brackets check still reports a missing bracket`() {
    checkAndGetIssues("hello-world", "fr").assert.containsExactly("BRACKETS_MISMATCH:QA_BRACKETS_MISSING")
    checkAndGetIssues("hello-world", "ja").assert.containsExactly("BRACKETS_MISMATCH:QA_BRACKETS_MISSING")
  }

  private fun checkAndGetIssues(
    keyName: String,
    languageTag: String,
  ): List<String> {
    val translation = testData.translations.getValue(keyName to languageTag)
    qaCheckBatchService.runChecksAndPersist(testData.project.id, translation.key.id, translation.language.id)

    return executeInNewTransaction(platformTransactionManager) {
      qaIssueRepository
        .findAllByTranslationId(translation.id)
        .filter { it.type in QaLanguageAwareChecksTestData.LANGUAGE_AWARE_CHECKS }
        .map { listOfNotNull(it.type.name, it.message.name, it.pluralVariant).joinToString(":") }
    }
  }
}
