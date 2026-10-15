package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.data.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleMatcher
import io.tolgee.ee.service.aiReview.AiReviewRuleMatcher.KeyScope
import io.tolgee.ee.service.aiReview.AiReviewRuleMatcher.Match
import io.tolgee.model.aiReview.AiReviewLanguageThreshold
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AiReviewRuleMatcherTest {
  private val testData = AiReviewTestData()
  private var nextId = 1L

  private fun rule(ft: AiReviewRule.() -> Unit = {}): AiReviewRule =
    testData
      .addRule {
        id = nextId++
        ft()
      }.self

  private val untagged = KeyScope(tags = emptySet(), namespace = null)
  private val ui = KeyScope(tags = setOf("ui"), namespace = null)
  private val legal = KeyScope(tags = setOf("legal"), namespace = null)
  private val uiLegal = KeyScope(tags = setOf("ui", "legal"), namespace = null)
  private val checkout = KeyScope(tags = emptySet(), namespace = "checkout")

  @Test
  fun `empty scope matches everything`() {
    val rule = rule()
    AiReviewRuleMatcher.matches(rule, untagged, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, uiLegal, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, checkout, LANGUAGE).assert.isTrue()
  }

  @Test
  fun `tag include matches keys having any of the tags`() {
    val rule = rule { tagIn = mutableListOf("ui") }
    AiReviewRuleMatcher.matches(rule, ui, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, uiLegal, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, legal, LANGUAGE).assert.isFalse()
    AiReviewRuleMatcher.matches(rule, untagged, LANGUAGE).assert.isFalse()
  }

  @Test
  fun `empty tag in include list means keys without tags`() {
    val rule = rule { tagIn = mutableListOf("") }
    AiReviewRuleMatcher.matches(rule, untagged, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, ui, LANGUAGE).assert.isFalse()

    val uiOrUntagged = rule { tagIn = mutableListOf("", "ui") }
    AiReviewRuleMatcher.matches(uiOrUntagged, untagged, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(uiOrUntagged, ui, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(uiOrUntagged, legal, LANGUAGE).assert.isFalse()
  }

  @Test
  fun `tag exclude also matches tags created later`() {
    val rule = rule { tagNotIn = mutableListOf("legal") }
    AiReviewRuleMatcher.matches(rule, ui, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, KeyScope(setOf("brand-new"), null), LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, untagged, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, legal, LANGUAGE).assert.isFalse()
    AiReviewRuleMatcher.matches(rule, uiLegal, LANGUAGE).assert.isFalse()
  }

  @Test
  fun `empty tag in exclude list excludes keys without tags`() {
    val rule = rule { tagNotIn = mutableListOf("") }
    AiReviewRuleMatcher.matches(rule, untagged, LANGUAGE).assert.isFalse()
    AiReviewRuleMatcher.matches(rule, ui, LANGUAGE).assert.isTrue()
  }

  @Test
  fun `namespace include and exclude handle the default namespace`() {
    val inCheckout = rule { namespaceIn = mutableListOf("checkout") }
    AiReviewRuleMatcher.matches(inCheckout, checkout, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(inCheckout, untagged, LANGUAGE).assert.isFalse()

    val inDefault = rule { namespaceIn = mutableListOf("") }
    AiReviewRuleMatcher.matches(inDefault, untagged, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(inDefault, checkout, LANGUAGE).assert.isFalse()

    val notDefault = rule { namespaceNotIn = mutableListOf("") }
    AiReviewRuleMatcher.matches(notDefault, untagged, LANGUAGE).assert.isFalse()
    AiReviewRuleMatcher.matches(notDefault, checkout, LANGUAGE).assert.isTrue()
  }

  @Test
  fun `language include list restricts languages`() {
    val rule = rule { languageIds = mutableListOf(LANGUAGE) }
    AiReviewRuleMatcher.matches(rule, ui, LANGUAGE).assert.isTrue()
    AiReviewRuleMatcher.matches(rule, ui, OTHER_LANGUAGE).assert.isFalse()
  }

  @Test
  fun `disabled rules are ignored`() {
    val rule = rule { enabled = false }
    AiReviewRuleMatcher.matches(rule, ui, LANGUAGE).assert.isFalse()
    AiReviewRuleMatcher.match(listOf(rule), ui, LANGUAGE).assert.isEqualTo(Match.NoRule)
  }

  @Test
  fun `returns no rule when nothing matches`() {
    val rule = rule { tagIn = mutableListOf("legal") }
    AiReviewRuleMatcher.match(listOf(rule), ui, LANGUAGE).assert.isEqualTo(Match.NoRule)
    AiReviewRuleMatcher.match(emptyList(), ui, LANGUAGE).assert.isEqualTo(Match.NoRule)
  }

  @Test
  fun `never beats any number`() {
    val strict = rule { threshold = 99 }
    val never = rule { languageThresholds = mutableListOf(AiReviewLanguageThreshold(LANGUAGE, never = true)) }
    AiReviewRuleMatcher.match(listOf(strict, never), ui, LANGUAGE).assert.isEqualTo(Match.Never(never))
    AiReviewRuleMatcher.match(listOf(strict, never), ui, OTHER_LANGUAGE).assert.isEqualTo(Match.Review(strict, 99))
  }

  @Test
  fun `highest effective threshold wins`() {
    val lenient = rule { threshold = 70 }
    val strict = rule { threshold = 90 }
    AiReviewRuleMatcher.match(listOf(lenient, strict), ui, LANGUAGE).assert.isEqualTo(Match.Review(strict, 90))
  }

  @Test
  fun `language override replaces the rule threshold`() {
    val overridden =
      rule {
        threshold = 70
        languageThresholds = mutableListOf(AiReviewLanguageThreshold(LANGUAGE, threshold = 95))
      }
    val strict = rule { threshold = 90 }
    AiReviewRuleMatcher.effectiveThreshold(overridden, LANGUAGE).assert.isEqualTo(95)
    AiReviewRuleMatcher.effectiveThreshold(overridden, OTHER_LANGUAGE).assert.isEqualTo(70)
    AiReviewRuleMatcher.match(listOf(overridden, strict), ui, LANGUAGE).assert.isEqualTo(Match.Review(overridden, 95))
    AiReviewRuleMatcher.match(listOf(overridden, strict), ui, OTHER_LANGUAGE).assert.isEqualTo(Match.Review(strict, 90))
  }

  @Test
  fun `ties go to the lowest rule id`() {
    val first = rule { threshold = 80 }
    val second = rule { threshold = 80 }
    AiReviewRuleMatcher.match(listOf(second, first), ui, LANGUAGE).assert.isEqualTo(Match.Review(first, 80))
  }

  @Test
  fun `mode does not influence precedence`() {
    val dryRunStrict =
      rule {
        threshold = 95
        mode = io.tolgee.model.enums.aiReview.AiReviewRuleMode.DRY_RUN
      }
    val active = rule { threshold = 80 }
    AiReviewRuleMatcher
      .match(
        listOf(active, dryRunStrict),
        ui,
        LANGUAGE,
      ).assert
      .isEqualTo(Match.Review(dryRunStrict, 95))
  }

  companion object {
    private const val LANGUAGE = 10L
    private const val OTHER_LANGUAGE = 20L
  }
}
