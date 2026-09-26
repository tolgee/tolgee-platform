package io.tolgee.ee.service.translationMemory

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TranslationMemoryTiebreakTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.translation.TranslationMemoryService
import io.tolgee.service.translationMemory.TmAutoTranslateProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@SpringBootTest
@Transactional
class TranslationMemoryTiebreakTest : AbstractSpringTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var tmAutoTranslateProvider: TmAutoTranslateProvider

  @Autowired
  private lateinit var translationMemoryService: TranslationMemoryService

  private lateinit var testData: TranslationMemoryTiebreakTestData

  @BeforeEach
  fun setup() {
    testData = TranslationMemoryTiebreakTestData()
    testDataService.saveTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TRANSLATION_MEMORY)
    age(testData.morningStaleTranslation)
  }

  @AfterEach
  fun cleanup() {
    enabledFeaturesProvider.forceEnabled = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `auto-translate prefers reviewed match over unreviewed`() {
    assertThat(autoTranslated(testData.helloTargetKey)).isEqualTo("Willkommen")
  }

  @Test
  fun `auto-translate prefers newest match when none is reviewed`() {
    assertThat(autoTranslated(testData.morningTargetKey)).isEqualTo("Guten Morgen")
  }

  @Test
  fun `classic exact-match lookup prefers reviewed match over unreviewed`() {
    assertThat(classicMatch(testData.helloTargetKey)).isEqualTo("Willkommen")
  }

  @Test
  fun `classic exact-match lookup prefers newest match when none is reviewed`() {
    assertThat(classicMatch(testData.morningTargetKey)).isEqualTo("Guten Morgen")
  }

  @Test
  fun `suggestions rank reviewed match first among equal similarity`() {
    val targets = suggestions("Welcome").map { it.targetTranslationText }
    assertThat(targets).containsExactly("Willkommen", "Welcome")
  }

  @Test
  fun `suggestions rank newest match first when none is reviewed`() {
    val targets = suggestions("Good morning").map { it.targetTranslationText }
    assertThat(targets).containsExactly("Guten Morgen", "Einen guten Morgen")
  }

  private fun autoTranslated(key: Key) =
    tmAutoTranslateProvider
      .getAutoTranslatedValue(keyService.get(key.id), testData.germanLanguage)
      ?.targetTranslationText

  private fun classicMatch(key: Key) =
    translationService
      .getTranslationMemoryValue(keyService.get(key.id), testData.germanLanguage)
      ?.targetTranslationText

  private fun suggestions(baseText: String) =
    translationMemoryService.getSuggestionsList(
      baseTranslationText = baseText,
      isPlural = false,
      keyId = null,
      projectId = testData.project.id,
      organizationId = testData.project.organizationOwner.id,
      targetLanguageTag = "de",
      limit = 10,
    )

  private fun age(translation: Translation) {
    entityManager
      .createNativeQuery("update translation set updated_at = :updatedAt where id = :id")
      .setParameter("updatedAt", Date(System.currentTimeMillis() - 86_400_000))
      .setParameter("id", translation.id)
      .executeUpdate()
  }
}
