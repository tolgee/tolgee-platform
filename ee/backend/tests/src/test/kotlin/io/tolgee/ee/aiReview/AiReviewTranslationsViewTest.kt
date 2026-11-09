package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTranslationsViewTestData
import io.tolgee.ee.service.aiReview.AiReviewEligibility
import io.tolgee.ee.service.aiReview.AiReviewRuleEvaluator
import io.tolgee.ee.service.aiReview.AiReviewRuleStatsService
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

  @org.springframework.beans.factory.annotation.Autowired
  private lateinit var statsService: AiReviewRuleStatsService

  lateinit var testData: AiReviewTranslationsViewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTranslationsViewTestData()
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
      it.de.self.state = TranslationState.REVIEWED
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
    keysFor("filterAiReviewStatus=CONFIRMED").assert.containsExactly("k.confirmed")
    keysFor("filterAiReviewStatus=REJECTED").assert.containsExactly("k.rejected")
  }

  @Test
  fun `the BELOW_THRESHOLD filter lists Reviewed texts whatever auto is and the FAILED filter skips them`() {
    listOf("k.humanTextBelow" to AiReviewStatus.BELOW_THRESHOLD, "k.reviewedFailed" to AiReviewStatus.FAILED)
      .forEach { (name, status) ->
        val key = testData.addKeyWithTranslations(name)
        key.de.self.state = TranslationState.REVIEWED
        testData.addReview(key.de, status, 40) { rule = testData.rule }
      }
    testData.addKeyWithTranslations("k.aiBelow").let {
      it.de.self.state = TranslationState.REVIEWED
      it.de.self.auto = true
      testData.addReview(it.de, AiReviewStatus.BELOW_THRESHOLD, 40) { rule = testData.rule }
    }
    save()
    keysFor("filterAiReviewStatus=BELOW_THRESHOLD")
      .assert
      .containsExactlyInAnyOrder("k.below", "k.humanTextBelow", "k.aiBelow")
    keysFor("filterAiReviewStatus=FAILED").assert.containsExactly("k.failed")
  }

  @Test
  fun `an APPROVED row on a Translated translation shows no badge, is not filtered and is not counted`() {
    testData.addKeyWithTranslations("k.unreviewed").let {
      testData.addReview(it.de, AiReviewStatus.APPROVED, 90) { rule = testData.rule }
    }
    save()
    fetch("search=k.unreviewed").andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.de.aiReview").isNull()
    }
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI").assert.containsExactly("k.approved")
    statsService
      .getCounts(testData.project.id)[testData.rule.id]!!
      .autoReviewed.assert
      .isEqualTo(0)
  }

  @Test
  fun `the outcome filters and the cell summaries agree for every state`() {
    val statuses = listOf(AiReviewStatus.APPROVED, AiReviewStatus.BELOW_THRESHOLD, AiReviewStatus.FAILED)
    statuses.forEach { status ->
      listOf(TranslationState.TRANSLATED, TranslationState.REVIEWED).forEach { state ->
        listOf(true, false).forEach { auto ->
          testData.addKeyWithTranslations("p.$status.$state.$auto").let {
            it.de.self.state = state
            it.de.self.auto = auto
            testData.addReview(it.de, status, 50) { rule = testData.rule }
          }
        }
      }
    }
    save()

    val summaries = summaryStatusesOfKeys("search=p.")
    mapOf(
      "REVIEWED_BY_AI" to AiReviewStatus.APPROVED,
      "BELOW_THRESHOLD" to AiReviewStatus.BELOW_THRESHOLD,
      "FAILED" to AiReviewStatus.FAILED,
    ).forEach { (outcome, status) ->
      val filtered = keysFor("filterAiReviewStatus=$outcome").filter { it.startsWith("p.") }
      val withBadge = summaries.filterValues { it == status.name }.keys
      filtered.assert.containsExactlyInAnyOrderElementsOf(withBadge)
    }
  }

  @Test
  fun `a whitespace-only text is not reviewable in the filter, the backlog or the eligibility check`() {
    val blank = testData.addKeyWithTranslations("k.blank", deText = "  \n ").de
    save()
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de").assert.doesNotContain("k.blank")
    val backlog = mutableListOf<Long>()
    statsService.forEachBacklogItem(testData.project.id, testData.rule.id) { backlog += it }
    backlog.assert.doesNotContain(blank.self.id)
    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.get(blank.self.id)
      AiReviewEligibility
        .isEligible(translation, testData.englishTranslationBuilder.self.language.id, null)
        .assert
        .isFalse()
    }
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
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de")
      .assert
      .contains("k.stalehash")
  }

  @Test
  fun `a filter aimed only at the base language returns no keys`() {
    save()
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI&filterAiReviewInLang=en").assert.isEmpty()
  }

  @Test
  fun `NOT_REVIEWED_YET uses the default branch name when the project has no branch rows`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.BRANCHING)
    testData.project.useBranching = true
    testData.rule.branchIn = mutableListOf("main")
    save()
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de")
      .assert
      .contains("checkout.pay")
  }

  @Test
  fun `NOT_REVIEWED_YET returns covered translations without a result`() {
    save()
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de").assert.containsExactlyInAnyOrder(
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

    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de&branch=fix_1")
      .assert
      .contains("k.onbranch")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de&branch=fixX1")
      .assert
      .doesNotContain("k.offbranch")
  }

  @Test
  fun `two outcomes combine with OR and the language filter limits the search`() {
    testData.addKeyWithTranslations("k.fr-only").let {
      it.fr.self.state = TranslationState.REVIEWED
      testData.addReview(it.fr, AiReviewStatus.APPROVED, 91)
    }
    save()
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI&filterAiReviewStatus=FAILED")
      .assert
      .containsExactlyInAnyOrder("k.approved", "k.failed", "k.fr-only")
    keysFor("filterAiReviewStatus=REVIEWED_BY_AI&filterAiReviewInLang=de")
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

    val keys = keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de")
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

    val keys = keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de")
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

    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de&branch=main")
      .assert
      .contains("k.onmain")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de&branch=fix_1")
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

    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de")
      .assert
      .contains("checkout.pay")
    keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=fr")
      .assert
      .isEmpty()
  }

  @Test
  fun `the count query agrees with the page content for NOT_REVIEWED_YET`() {
    save()
    val query = "filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de"
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
      keysFor("filterAiReviewStatus=NOT_REVIEWED_YET&filterAiReviewInLang=de")
        .filter { it.startsWith("parity.") }
        .toSet()
    sqlKeys.assert.isEqualTo(expected)
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

  private fun summaryStatusesOfKeys(query: String): Map<String, String?> {
    val response =
      fetch(query)
        .andIsOk
        .andReturn()
        .response.contentAsString
    val keys = jacksonObjectMapper().readTree(response).path("_embedded").path("keys")
    return (0 until keys.size()).associate {
      val key = keys.get(it)
      val status =
        key
          .path("translations")
          .path("de")
          .path("aiReview")
          .path("status")
      key.path("keyName").asString() to status.takeIf { node -> node.isString }?.asString()
    }
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
