package io.tolgee.ee.api.v2.controllers.translationMemory

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.service.translationMemory.ManagedTranslationMemorySuggestionService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

/**
 * Exercises the unified `/suggest/translation-memory` endpoint against the new managed TM path
 * (entries from `translation_memory_entry`). The legacy path (queries the `translation` table)
 * is covered by [TranslationSuggestionControllerTmTest] which uses a project without a project TM.
 *
 * Plan-aware behavior:
 * - Free plan → suggestions only from the project's own PROJECT-type TM (shared TMs invisible).
 * - Paid plan (`Feature.TRANSLATION_MEMORY`) → project TM + assigned shared TMs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TranslationSuggestionControllerManagedTmTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var managedTranslationMemorySuggestionService: ManagedTranslationMemorySuggestionService

  lateinit var testData: TranslationMemoryTestData
  var germanLanguageId: Long = 0

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectWithTm }
    germanLanguageId = testData.germanLanguageWithTm.id
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `paid plan returns suggestions from shared TM`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Hello world",
        targetLanguageId = germanLanguageId,
        isPlural = false,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryItems[0].baseText").isEqualTo("Hello world")
        node("_embedded.translationMemoryItems[0].targetText").isEqualTo("Hallo Welt")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `paid plan returns suggestions from project TM`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Existing source",
        targetLanguageId = germanLanguageId,
        isPlural = false,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryItems[0].targetText").isEqualTo("Bestehende Übersetzung")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `free plan excludes shared TM from suggestions`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      SuggestRequestDto(
        baseText = "Hello world",
        targetLanguageId = germanLanguageId,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        // "Hello world" lives in the shared TM — invisible on free plan
        node("page.totalElements").isEqualTo(0)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `free plan returns suggestions from project TM`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    val request =
      SuggestRequestDto(
        baseText = "Existing source",
        targetLanguageId = germanLanguageId,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryItems[0].targetText").isEqualTo("Bestehende Übersetzung")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns empty page when no similar text exists`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Completely unrelated quantum entanglement",
        targetLanguageId = germanLanguageId,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `shared TM default penalty lowers displayed similarity but leaves raw intact`() {
    // TestData.sharedTmWithPenalty has defaultPenalty=25 and entry "Farewell"→"Auf Wiedersehen".
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Farewell",
        targetLanguageId = germanLanguageId,
        isPlural = false,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.translationMemoryItems[0].targetText").isEqualTo("Auf Wiedersehen")
        node("_embedded.translationMemoryItems[0].similarity").isNumber.satisfies({
          assert(it.toFloat() == 0.75f) { "expected 1.00 - 0.25 = 0.75, got $it" }
        })
        node("_embedded.translationMemoryItems[0].rawSimilarity").isNumber.satisfies({
          assert(it.toFloat() == 1.0f) { "expected raw similarity = 1.00, got $it" }
        })
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `per-assignment penalty overrides TM default`() {
    // TestData.sharedTmWithOverride: defaultPenalty=10, assignment.penalty=40, entry "Good luck"→"Viel Glück".
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Good luck",
        targetLanguageId = germanLanguageId,
        isPlural = false,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        // override of 40 wins over default of 10 → 1.00 - 0.40 = 0.60
        node("_embedded.translationMemoryItems[0].targetText").isEqualTo("Viel Glück")
        node("_embedded.translationMemoryItems[0].similarity").isNumber.satisfies({
          assert(it.toFloat() == 0.6f) { "expected 0.60, got $it" }
        })
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project TM ignores defaultPenalty`() {
    // TestData.projectTm has defaultPenalty=50 preset in the fixture. The SQL still
    // returns similarity=raw for PROJECT-type TMs regardless of that value.
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Existing source",
        targetLanguageId = germanLanguageId,
        isPlural = false,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        // Project TM ignores penalty entirely: similarity == rawSimilarity == 1.0
        node("_embedded.translationMemoryItems[0].similarity").isNumber.satisfies({
          assert(it.toFloat() == 1.0f) { "expected 1.00 (project TM ignores penalty), got $it" }
        })
        node("_embedded.translationMemoryItems[0].rawSimilarity").isNumber.satisfies({
          assert(it.toFloat() == 1.0f) { "expected raw = 1.00, got $it" }
        })
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `managed getSuggestionsList returns shared TM entries with penalty applied for MT context`() {
    // TestData.sharedTmWithPenalty (defaultPenalty=25, entry "Farewell"→"Auf Wiedersehen")
    // stands in for the realistic MT-context scenario: a shared TM trusted less than the
    // project's own memory contributes weaker context to the MT prompt.
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)

    val results =
      managedTranslationMemorySuggestionService.getSuggestionsList(
        baseTranslationText = "Farewell",
        isPlural = false,
        keyId = null,
        projectId = testData.projectWithTm.id,
        organizationId = testData.projectWithTm.organizationOwner.id,
        targetLanguageTag = "de",
        limit = 5,
      )

    val match = results.first { it.baseTranslationText == "Farewell" }
    assertThat(match.targetTranslationText).isEqualTo("Auf Wiedersehen")
    // defaultPenalty=25 → 1.00 - 0.25 = 0.75
    assertThat(match.rawSimilarity).isEqualTo(1.0f)
    assertThat(match.similarity).isEqualTo(0.75f)
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `requires TRANSLATIONS_EDIT scope to call suggestion endpoint`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    val request =
      SuggestRequestDto(
        baseText = "Hello world",
        targetLanguageId = germanLanguageId,
      )
    performProjectAuthPost("suggest/translation-memory", request).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `identical entries across TMs are deduped, highest-priority TM survives`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    executeInNewTransaction {
      val project = projectService.get(testData.projectWithTm.id)
      val german = languageService.findEntitiesByTags(setOf("de"), project.id).first()
      val key =
        keyService.create(
          project,
          CreateKeyDto("priority-tie-test", null, mapOf("en" to "Priority tie test")),
        )
      val translation = translationService.getOrCreate(key, german)
      translation.text = "Prioritätstest"
      translationService.save(translation)
    }

    val request =
      SuggestRequestDto(
        baseText = "Priority tie test",
        targetLanguageId = germanLanguageId,
        isPlural = false,
      )
    performAuthPost("/v2/projects/${project.id}/suggest/translation-memory", request)
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
        // Project TM sits at priority 0 (top) by default and therefore wins the dedup when
        // pair `Priority tie test → Prioritätstest (de)` surfaces from both it (virtual) and a
        // shared TM (priority 1 in the fixture).
        node("_embedded.translationMemoryItems[0].translationMemoryName")
          .isEqualTo(testData.projectTm.name)
        node("_embedded.translationMemoryItems[0].targetText")
          .isEqualTo("Prioritätstest")
      }
  }
}
