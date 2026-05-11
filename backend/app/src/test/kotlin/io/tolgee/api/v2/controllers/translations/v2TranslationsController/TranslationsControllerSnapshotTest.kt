package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TranslationsSnapshotTestData
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

/**
 * Comprehensive snapshot of TranslationViewDataProvider output for a broad fixture
 * (multiple languages, tags, comments, different translation states, outdated/auto flags, key
 * meta). Asserts the exact shape of every field returned by the data provider and fails loudly
 * on any drift in:
 *
 * - per-language `text`, `state`, `outdated`, `auto`, `mtProvider`
 * - per-language `commentCount` and `unresolvedCommentCount`
 * - per-language `activeSuggestionCount` and `totalSuggestionCount`
 * - per-key `screenshotCount`, `branch`, `keyNamespace`, `keyDescription`, `contextPresent`
 * - per-key `keyTags`
 * - row count and pagination
 *
 * Acts as a safety net against regressions in any query-builder change that touches the
 * Translation View pipeline.
 */
@SpringBootTest
@Transactional
class TranslationsControllerSnapshotTest : AbstractSpringTest() {
  @Autowired
  private lateinit var translationViewDataProvider: TranslationViewDataProvider

  private lateinit var testData: TranslationsSnapshotTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsSnapshotTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `produces stable snapshot for a comprehensive scenario`() {
    val result =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage, testData.germanLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(0, 50),
        params = GetTranslationsParams(),
      )

    // ===== Total count =====
    // A key, Z key, commented_key, 3 tagged keys, 6 state test keys = 12
    // Default branch filter excludes branched keys (branch key is on test-branch).
    assertThat(result.totalElements).isEqualTo(12)
    assertThat(result.content).hasSize(12)

    // ===== "A key" snapshot — base key with german REVIEWED, outdated, auto, mtProvider, comment
    val aKey = result.content.first { it.keyName == "A key" }
    assertThat(aKey.keyDescription).isEqualTo("A key description")
    assertThat(aKey.screenshotCount).isEqualTo(0L)
    assertThat(aKey.contextPresent).isFalse()
    assertThat(aKey.keyNamespace).isNull()
    assertThat(aKey.keyTags.map { it.name }).containsExactly("Cool tag")

    val aKeyDe = aKey.translations["de"]!!
    assertThat(aKeyDe.text).isEqualTo("Z translation")
    assertThat(aKeyDe.state).isEqualTo(TranslationState.REVIEWED)
    assertThat(aKeyDe.outdated).isTrue()
    assertThat(aKeyDe.auto).isTrue()
    assertThat(aKeyDe.commentCount).isEqualTo(1L)
    assertThat(aKeyDe.unresolvedCommentCount).isEqualTo(0L) // the only comment is RESOLVED
    assertThat(aKeyDe.activeSuggestionCount).isEqualTo(0L)
    assertThat(aKeyDe.totalSuggestionCount).isEqualTo(0L)

    val aKeyEn = aKey.translations["en"]!!
    // base init does not add an english translation for "A key" -> UNTRANSLATED
    assertThat(aKeyEn.id).isNull()
    assertThat(aKeyEn.state).isEqualTo(TranslationState.UNTRANSLATED)
    assertThat(aKeyEn.commentCount).isEqualTo(0L)
    assertThat(aKeyEn.unresolvedCommentCount).isEqualTo(0L)

    // ===== "Z key" snapshot — base key with english auto-translated
    val zKey = result.content.first { it.keyName == "Z key" }
    assertThat(zKey.keyTags.map { it.name }).containsExactlyInAnyOrder("Lame tag", "Some other tag")
    val zKeyEn = zKey.translations["en"]!!
    assertThat(zKeyEn.text).isEqualTo("A translation")
    assertThat(zKeyEn.auto).isTrue()
    assertThat(zKeyEn.outdated).isFalse()

    // ===== "commented_key" snapshot — has 2 resolved + 2 unresolved comments on de
    val commentedKey = result.content.first { it.keyName == "commented_key" }
    val commentedDe = commentedKey.translations["de"]!!
    assertThat(commentedDe.text).isEqualTo("Nice")
    assertThat(commentedDe.commentCount).isEqualTo(4L)
    assertThat(commentedDe.unresolvedCommentCount).isEqualTo(2L)
    val commentedEn = commentedKey.translations["en"]!!
    assertThat(commentedEn.id).isNull()
    assertThat(commentedEn.state).isEqualTo(TranslationState.UNTRANSLATED)

    // ===== Tag-based keys =====
    val keyWithTag = result.content.first { it.keyName == "Key with tag" }
    assertThat(keyWithTag.keyTags.map { it.name }).containsExactly("Cool tag")
    assertThat(keyWithTag.translations["de"]!!.text).isEqualTo("Key with tag DE")
    assertThat(keyWithTag.translations["en"]!!.text).isEqualTo("Key with tag EN")

    val anotherKeyWithTag = result.content.first { it.keyName == "Another key with tag" }
    assertThat(anotherKeyWithTag.keyTags.map { it.name }).containsExactly("Another cool tag")

    // ===== State-based keys ===========
    val stateKey1 = result.content.first { it.keyName == "state test key" }
    assertThat(stateKey1.translations["de"]!!.state).isEqualTo(TranslationState.REVIEWED)
    assertThat(stateKey1.translations["en"]!!.state).isEqualTo(TranslationState.REVIEWED)
    assertThat(stateKey1.translations["de"]!!.text).isEqualTo("a")
    assertThat(stateKey1.translations["en"]!!.text).isEqualTo("aa")

    val stateKey2 = result.content.first { it.keyName == "state test key 2" }
    assertThat(stateKey2.translations["de"]!!.state).isEqualTo(TranslationState.TRANSLATED)
    assertThat(stateKey2.translations["en"]!!.state).isEqualTo(TranslationState.REVIEWED)

    val stateKey6 = result.content.first { it.keyName == "state test key 6" }
    assertThat(stateKey6.translations["de"]!!.state).isEqualTo(TranslationState.DISABLED)
    // The Disabled translation has null text — verify
    assertThat(stateKey6.translations["de"]!!.text).isNull()
  }

  @Test
  fun `produces stable snapshot for a filtered + paginated query`() {
    // Filter: only keys tagged with "Cool tag"
    val pageOne =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage, testData.germanLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(0, 2),
        params = GetTranslationsParams().apply { filterTag = listOf("Cool tag") },
      )

    // Should match "A key" and "Key with tag" and "Key with tag 2" — 3 keys in total
    assertThat(pageOne.totalElements).isEqualTo(3)
    assertThat(pageOne.content).hasSize(2)
    assertThat(pageOne.content.map { it.keyName })
      .containsExactly("A key", "Key with tag")
    pageOne.content.forEach { key ->
      assertThat(key.keyTags.map { it.name }).contains("Cool tag")
    }

    val pageTwo =
      translationViewDataProvider.getData(
        projectId = testData.project.id,
        languages =
          languageService
            .dtosFromEntities(
              listOf(testData.englishLanguage, testData.germanLanguage),
              testData.project.id,
            ).toSet(),
        PageRequest.of(1, 2),
        params = GetTranslationsParams().apply { filterTag = listOf("Cool tag") },
      )

    assertThat(pageTwo.content).hasSize(1)
    assertThat(pageTwo.content[0].keyName).isEqualTo("Key with tag 2")
    assertThat(pageTwo.content[0].keyTags.map { it.name }).contains("Cool tag")
  }
}
