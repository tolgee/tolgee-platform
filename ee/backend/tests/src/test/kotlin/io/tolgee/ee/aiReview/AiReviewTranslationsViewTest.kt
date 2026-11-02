package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleEvaluator
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.ResultActions
import tools.jackson.module.kotlin.jacksonObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewTranslationsViewTest : AuthorizedControllerTest() {
  @org.springframework.beans.factory.annotation.Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @org.springframework.beans.factory.annotation.Autowired
  private lateinit var evaluator: AiReviewRuleEvaluator

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    row("k.approved", AiReviewStatus.APPROVED, 90)
    row("k.below", AiReviewStatus.BELOW_THRESHOLD, 40)
    row("k.drypass", AiReviewStatus.DRY_RUN_PASS, 90)
    row("k.dryfail", AiReviewStatus.DRY_RUN_FAIL, 30)
    row("k.failed", AiReviewStatus.FAILED, null)
    row("k.pending", AiReviewStatus.PENDING, null)
    row("k.skipped", AiReviewStatus.SKIPPED_QA_ISSUES, null)
    testData.addKeyWithTranslations("k.none")
    testData.addKeyWithTranslations("k.untagged", tags = emptyList())
    testData.addRule {
      name = "Never legal"
      never = true
      tagIn = mutableListOf("legal")
    }
    testData.addKeyWithTranslations("k.never", tags = listOf("ui", "legal"))
    testData
      .addKeyWithTranslations("k.outdated")
      .de.self.outdated = true
    testData
      .addKeyWithTranslations("k.reviewed")
      .de.self.state = TranslationState.REVIEWED
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `the translations list contains the aiReview summary for current rows`() {
    save()
    fetch("search=k.approved").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview.status").isEqualTo("APPROVED")
      node("_embedded.keys[0].translations.de.aiReview.score").isEqualTo(90)
      node("_embedded.keys[0].translations.de.aiReview.dryRun").isEqualTo(false)
      node("_embedded.keys[0].translations.de.aiReview.lastFailed").isEqualTo(false)
    }
    fetch("search=k.none").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview").isNull()
    }
  }

  @Test
  fun `the summary reports lastFailed and dryRun`() {
    testData.addKeyWithTranslations("k.lastfailed").let {
      testData.addReview(it.de, AiReviewStatus.APPROVED, 88) {
        lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
      }
    }
    save()
    fetch("search=k.lastfailed").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview.lastFailed").isEqualTo(true)
    }
    fetch("search=k.drypass").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview.dryRun").isEqualTo(true)
      node("_embedded.keys[0].translations.de.aiReview.status").isEqualTo("DRY_RUN_PASS")
    }
  }

  @Test
  fun `the summary is null without the feature`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    fetch("search=k.approved").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview").isNull()
    }
  }

  @Test
  fun `the summary is null for a stale hash`() {
    testData.addKeyWithTranslations("k.stale").let {
      testData.addReview(it.de, AiReviewStatus.APPROVED, 90) {
        textHash = "0000000000000000000000000000000000000000000000000000000000000000"
      }
    }
    save()
    fetch("search=k.stale").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview").isNull()
    }
  }

  @Test
  fun `each outcome filter returns only its keys`() {
    save()
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI").assert.containsExactly("k.approved")
    keysFor("filterAiReviewStatus=BELOW_THRESHOLD").assert.containsExactly("k.below")
    keysFor("filterAiReviewStatus=DRY_RUN_PASS").assert.containsExactly("k.drypass")
    keysFor("filterAiReviewStatus=DRY_RUN_FAIL").assert.containsExactly("k.dryfail")
    keysFor("filterAiReviewStatus=FAILED").assert.containsExactly("k.failed")
  }

  @Test
  fun `the outcome filters match only rows for the current text`() {
    testData.addKeyWithTranslations("k.stalehash").let {
      testData.addReview(it.de, AiReviewStatus.APPROVED, 90) {
        textHash = "0000000000000000000000000000000000000000000000000000000000000000"
      }
    }
    save()
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI").assert.containsExactly("k.approved")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de")
      .assert
      .contains("k.stalehash")
  }

  @Test
  fun `a filter aimed only at the base language returns no keys`() {
    save()
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI&filterAiReviewLanguage=en").assert.isEmpty()
  }

  @Test
  fun `NOT_REVIEWED_YET uses the default branch name when the project has no branch rows`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.BRANCHING)
    testData.project.useBranching = true
    testData.rule.branchIn = mutableListOf("main")
    save()
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de")
      .assert
      .contains("checkout.pay")
  }

  @Test
  fun `NOT_REVIEWED_YET returns covered translations without a result`() {
    save()
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de").assert.containsExactlyInAnyOrder(
      "checkout.pay",
      "k.none",
      "k.pending",
      "k.skipped",
    )
  }

  @Test
  fun `NOT_REVIEWED_YET respects branch patterns with literal underscores`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.BRANCHING)
    testData.project.useBranching = true
    testData.rule.branchIn = mutableListOf("fix_1")
    val matching =
      testData.projectBuilder
        .addBranch { name = "fix_1" }
        .self
    val notMatching =
      testData.projectBuilder
        .addBranch { name = "fixX1" }
        .self
    testData
      .addKeyWithTranslations("k.onbranch")
      .keyBuilder.self.branch = matching
    testData
      .addKeyWithTranslations("k.offbranch")
      .keyBuilder.self.branch = notMatching
    save()

    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de&branch=fix_1")
      .assert
      .contains("k.onbranch")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de&branch=fixX1")
      .assert
      .doesNotContain("k.offbranch")
  }

  @Test
  fun `two outcomes combine with OR and the language filter limits the search`() {
    testData.addKeyWithTranslations("k.fr-only").let {
      testData.addReview(it.fr, AiReviewStatus.APPROVED, 91)
    }
    save()
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI&filterAiReviewStatus=FAILED")
      .assert
      .containsExactlyInAnyOrder("k.approved", "k.failed", "k.fr-only")
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI&filterAiReviewLanguage=de")
      .assert
      .containsExactly("k.approved")
  }

  @Test
  fun `the filters are ignored without the feature`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    val keys = keysFor("filterAiReviewStatus=REVIEWED_BY_AI")
    keys.size.assert.isGreaterThan(5)
  }

  @Test
  fun `NOT_REVIEWED_YET respects tag scopes including the without-tags value`() {
    testData.rule.tagNotIn = mutableListOf("legal")
    testData.addKeyWithTranslations("k.taggedout", tags = listOf("ui", "legal"))
    testData.addRule {
      name = "No tags"
      tagIn = mutableListOf("")
    }
    save()

    val keys = keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de")
    keys.assert.contains("checkout.pay")
    keys.assert.doesNotContain("k.taggedout")
    keys.assert.contains("k.untagged")
  }

  @Test
  fun `NOT_REVIEWED_YET respects namespace scopes including the default namespace`() {
    testData.rule.tagIn = mutableListOf()
    testData.rule.namespaceIn = mutableListOf("checkout", "")
    testData.rule.namespaceNotIn = mutableListOf("internal")
    testData.addKeyWithTranslations("k.inns").keyBuilder.setNamespace("checkout")
    testData.addKeyWithTranslations("k.outns").keyBuilder.setNamespace("internal")
    save()

    val keys = keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de")
    keys.assert.contains("k.inns")
    keys.assert.contains("checkout.pay")
    keys.assert.doesNotContain("k.outns")
  }

  @Test
  fun `NOT_REVIEWED_YET respects branchNotIn`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.BRANCHING)
    testData.project.useBranching = true
    testData.rule.branchNotIn = mutableListOf("fix_1")
    val excluded = testData.projectBuilder.addBranch { name = "fix_1" }.self
    val included =
      testData.projectBuilder
        .addBranch {
          name = "main"
          isDefault = true
        }.self
    testData
      .addKeyWithTranslations("k.onmain")
      .keyBuilder.self.branch = included
    testData
      .addKeyWithTranslations("k.onfix")
      .keyBuilder.self.branch = excluded
    save()

    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de&branch=main")
      .assert
      .contains("k.onmain")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de&branch=fix_1")
      .assert
      .doesNotContain("k.onfix")
  }

  @Test
  fun `NOT_REVIEWED_YET respects the rule languages and never overrides`() {
    testData.rule.languages = mutableSetOf(testData.german)
    testData
      .addRule {
        name = "French with never override"
        tagIn = mutableListOf("ui")
        languages = mutableSetOf(testData.french)
      }.addOverride(testData.french, never = true)
    save()

    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de")
      .assert
      .contains("checkout.pay")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=fr")
      .assert
      .isEmpty()
  }

  @Test
  fun `the count query agrees with the page content for NOT_REVIEWED_YET`() {
    save()
    val query = "filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de"
    val keys = keysFor(query)
    totalFor(query).assert.isEqualTo(keys.size.toLong())
  }

  @Test
  fun `the SQL filter agrees with the in-memory rule matcher`() {
    testData.rule.tagNotIn = mutableListOf("legal")
    testData.addRule {
      name = "No tags"
      tagIn = mutableListOf("")
    }
    testData.addRule {
      name = "Checkout namespace"
      namespaceIn = mutableListOf("checkout")
    }
    val tagCombinations =
      mapOf(
        "parity.ui" to listOf("ui"),
        "parity.uilegal" to listOf("ui", "legal"),
        "parity.legal" to listOf("legal"),
        "parity.none" to emptyList<String>(),
      )
    val deTranslations = mutableMapOf<String, io.tolgee.development.testDataBuilder.builders.TranslationBuilder>()
    tagCombinations.forEach { (name, tags) ->
      deTranslations[name] = testData.addKeyWithTranslations(name, tags = tags).de
    }
    deTranslations["parity.ns"] =
      testData
        .addKeyWithTranslations("parity.ns", tags = emptyList())
        .also { it.keyBuilder.setNamespace("checkout") }
        .de
    save()

    val expected =
      executeInNewTransaction(platformTransactionManager) {
        deTranslations
          .filterValues { builder ->
            evaluator.evaluate(translationService.get(builder.self.id)) is RuleDecision.Review
          }.keys
      }
    expected.assert.isNotEmpty()

    val sqlKeys =
      keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewLanguage=de")
        .filter { it.startsWith("parity.") }
        .toSet()
    sqlKeys.assert.isEqualTo(expected)
  }

  private fun row(
    keyName: String,
    status: AiReviewStatus,
    score: Int?,
  ) {
    val key = testData.addKeyWithTranslations(keyName)
    testData.addReview(key.de, status, score) {
      rule = testData.rule
      if (status == AiReviewStatus.FAILED) failureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
  }

  private fun fetch(query: String): ResultActions =
    performAuthGet("$projectUrl/translations?languages=en&languages=de&languages=fr&size=100&$query")

  private fun totalFor(query: String): Long {
    val response =
      fetch(query)
        .andIsOk
        .andReturn()
        .response.contentAsString
    return jacksonObjectMapper()
      .readTree(response)
      .path("page")
      .path("totalElements")
      .asLong()
  }

  private fun keysFor(query: String): List<String> {
    val response =
      fetch(query)
        .andIsOk
        .andReturn()
        .response.contentAsString
    val json = jacksonObjectMapper().readTree(response)
    val keys = json.path("_embedded").path("keys")
    if (keys.isMissingNode) return emptyList()
    return (0 until keys.size()).map { keys.get(it).path("keyName").asString() }
  }
}
