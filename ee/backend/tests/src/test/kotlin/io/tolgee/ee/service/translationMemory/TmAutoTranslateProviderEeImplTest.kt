package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.AutoTranslationConfigRepository
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

/**
 * Verifies that [AutoTranslationService] routes through the managed TM path for projects
 * whose organization has [Feature.TRANSLATION_MEMORY] enabled, and through the classic
 * `TranslationMemoryService` path otherwise.
 *
 * Scenario: the test project has a shared TM containing "Hello world" → "Hallo Welt".
 * - With feature ON: auto-translating a new "Hello world" key picks up the shared TM match.
 * - With feature OFF: the same setup yields no match because the classic path only knows
 *   about the `translation` table which doesn't contain "Hello world".
 */
@SpringBootTest
@AutoConfigureMockMvc
class TmAutoTranslateProviderEeImplTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var autoTranslationService: AutoTranslationService

  @Autowired
  private lateinit var autoTranslationConfigRepository: AutoTranslationConfigRepository

  @Autowired
  private lateinit var keyRepository: KeyRepository

  @Autowired
  private lateinit var translationRepository: TranslationRepository

  private lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
    // Enable auto-translate-via-TM for the project
    val config =
      AutoTranslationConfig().apply {
        project = testData.projectWithTm
        usingTm = true
      }
    autoTranslationConfigRepository.save(config)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `with feature enabled, auto-translate uses shared TM match`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)

    val germanId =
      executeInNewTransaction {
        val (newKey, germanTranslation) = createKeyWithEnglishSource("Hello world")
        autoTranslationService.softAutoTranslate(
          projectId = testData.projectWithTm.id,
          keyId = newKey.id,
          languageId = testData.germanLanguageWithTm.id,
        )
        germanTranslation.id
      }

    val after = translationRepository.findById(germanId).get()
    assertThat(after.text).isEqualTo("Hallo Welt")
    assertThat(after.auto).isTrue
  }

  @Test
  fun `with feature disabled, auto-translate falls back to classic path and finds no match`() {
    enabledFeaturesProvider.forceEnabled = emptySet()

    val germanId =
      executeInNewTransaction {
        val (newKey, germanTranslation) = createKeyWithEnglishSource("Hello world")
        autoTranslationService.softAutoTranslate(
          projectId = testData.projectWithTm.id,
          keyId = newKey.id,
          languageId = testData.germanLanguageWithTm.id,
        )
        germanTranslation.id
      }

    // Classic path queries the `translation` table for "Hello world" — no such entry exists
    // in projectWithTm's translations (only "Existing source" → "Bestehende Übersetzung" is
    // in the translation table; "Hello world" is only in the shared TM). So no auto-translation
    // happens — the German translation stays empty.
    val after = translationRepository.findById(germanId).get()
    assertThat(after.text.isNullOrBlank()).isTrue
  }

  /**
   * Creates a new key in [TranslationMemoryTestData.projectWithTm] with an English source and an
   * empty German translation row. Returns the new key and its untranslated German [Translation].
   */
  private fun createKeyWithEnglishSource(englishText: String): Pair<Key, Translation> {
    val project = testData.projectWithTm
    val english = languageService.findEntity(project.id, "en")!!
    val german = testData.germanLanguageWithTm

    val newKey =
      Key().apply {
        name = "hello-key"
        this.project = project
      }
    keyRepository.save(newKey)

    val englishTranslation =
      Translation().apply {
        key = newKey
        language = english
        text = englishText
      }
    translationRepository.save(englishTranslation)
    newKey.translations.add(englishTranslation)

    val germanTranslation =
      Translation().apply {
        key = newKey
        language = german
        text = ""
      }
    translationRepository.save(germanTranslation)
    newKey.translations.add(germanTranslation)

    return newKey to germanTranslation
  }
}
