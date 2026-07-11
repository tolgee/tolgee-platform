package io.tolgee.ee.service.translationMemory

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.repository.AutoTranslationConfigRepository
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
  private lateinit var translationRepository: TranslationRepository

  private lateinit var testData: TranslationMemoryTestData

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
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
    val germanId = testData.helloKeyGermanTranslation.id

    executeInNewTransaction {
      autoTranslationService.softAutoTranslate(
        projectId = testData.projectWithTm.id,
        keyId = testData.helloKey.id,
        languageId = testData.germanLanguageWithTm.id,
      )
    }

    val after = translationRepository.findById(germanId).get()
    assertThat(after.text).isEqualTo("Hallo Welt")
    assertThat(after.auto).isTrue
  }

  @Test
  fun `auto-translate fills target from another project's translation via virtual rows`() {
    // No stored entry on any TM has source 'Hello world' + lang cs. The cs translation
    // exists only on conflictProject.shared-greeting-cs. Both projects write to
    // multiProjectSharedTm and projectWithTm reads it — so the virtual-rows half of the EE
    // exact-match query is the only path that can resolve this auto-translate.
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val czechId = testData.helloKeyCzechTranslation.id

    executeInNewTransaction {
      autoTranslationService.softAutoTranslate(
        projectId = testData.projectWithTm.id,
        keyId = testData.helloKey.id,
        languageId = testData.czechLanguageWithTm.id,
      )
    }

    val after = translationRepository.findById(czechId).get()
    assertThat(after.text).isEqualTo("Ahoj světe")
    assertThat(after.auto).isTrue
  }

  @Test
  fun `auto-translate skips a TM with non-zero penalty`() {
    // sharedTmWithPenalty has defaultPenalty = 25 and is the only TM whose stored entries
    // carry source 'Farewell'. The suggestion panel would still show it (with similarity
    // reduced by the penalty), but auto-translate must refuse to fill — the user has marked
    // the TM as not fully trusted, so silent overwrites are unwanted.
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val germanId = testData.farewellKeyGermanTranslation.id

    executeInNewTransaction {
      autoTranslationService.softAutoTranslate(
        projectId = testData.projectWithTm.id,
        keyId = testData.farewellKey.id,
        languageId = testData.germanLanguageWithTm.id,
      )
    }

    val after = translationRepository.findById(germanId).get()
    assertThat(after.text.isNullOrBlank()).isTrue
  }

  @Test
  fun `auto-translate ignores soft-deleted source keys`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val germanId = testData.trashedSourceLiveTwinGermanTranslation.id

    executeInNewTransaction {
      autoTranslationService.softAutoTranslate(
        projectId = testData.projectWithTm.id,
        keyId = testData.trashedSourceLiveTwinKey.id,
        languageId = testData.germanLanguageWithTm.id,
      )
    }

    val after = translationRepository.findById(germanId).get()
    assertThat(after.text.isNullOrBlank()).isTrue
  }

  @Test
  fun `with feature disabled, auto-translate falls back to classic path and finds no match`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val germanId = testData.helloKeyGermanTranslation.id

    executeInNewTransaction {
      autoTranslationService.softAutoTranslate(
        projectId = testData.projectWithTm.id,
        keyId = testData.helloKey.id,
        languageId = testData.germanLanguageWithTm.id,
      )
    }

    // Classic path queries the `translation` table for "Hello world" — no such entry exists
    // in projectWithTm's translations (only "Existing source" → "Bestehende Übersetzung" is
    // in the translation table; "Hello world" is only in the shared TM). So no auto-translation
    // happens — the German translation stays empty.
    val after = translationRepository.findById(germanId).get()
    assertThat(after.text.isNullOrBlank()).isTrue
  }
}
