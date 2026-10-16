package io.tolgee.ee.aiReview

import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.util.aiReviewTextHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiReviewTranslationsViewTest : AbstractAiReviewTest() {
  @BeforeEach
  fun seed() {
    val rule = testData.addRule()

    fun seed(
      translation: TranslationBuilder,
      outcome: AiReviewOutcome,
      textHash: String = aiReviewTextHash(translation.self.text),
    ) {
      translation.addAiReview {
        this.outcome = outcome
        this.rule = rule.self
        this.textHash = textHash
        score = 92
        threshold = 85
      }
    }
    testData.uiGerman.self.state = TranslationState.REVIEWED
    seed(testData.uiGerman, AiReviewOutcome.AUTO_REVIEWED)
    seed(testData.uiCzech, AiReviewOutcome.BELOW_THRESHOLD, textHash = aiReviewTextHash("Some older text"))
    seed(testData.uiFrench, AiReviewOutcome.CONFIRMED)
    seed(testData.legalGerman, AiReviewOutcome.REJECTED)
    seed(testData.checkoutGerman, AiReviewOutcome.PENDING)
    seed(testData.plainGerman, AiReviewOutcome.WOULD_AUTO_REVIEW)
    saveTestData()
  }

  private fun getTranslations() = performAuthGet("/v2/projects/$projectId/translations?languages=de,cs,fr&sort=keyName")

  @Test
  fun `includes the AI review summary`() {
    getTranslations().andIsOk.andAssertThatJson {
      node("_embedded.keys[3].keyName").isEqualTo("ui-key")
      node("_embedded.keys[3].translations.de.aiReview.outcome").isEqualTo("AUTO_REVIEWED")
      node("_embedded.keys[3].translations.de.aiReview.score").isEqualTo(92)
      node("_embedded.keys[3].translations.de.aiReview.threshold").isEqualTo(85)
      node("_embedded.keys[3].translations.de.aiReview.dryRun").isEqualTo(false)
      node("_embedded.keys[2].keyName").isEqualTo("plain-key")
      node("_embedded.keys[2].translations.de.aiReview.outcome").isEqualTo("WOULD_AUTO_REVIEW")
    }
  }

  @Test
  fun `omits outdated, resolved and pending results`() {
    getTranslations().andIsOk.andAssertThatJson {
      node("_embedded.keys[3].translations.cs.aiReview").isNull()
      node("_embedded.keys[3].translations.fr.aiReview").isNull()
      node("_embedded.keys[1].keyName").isEqualTo("legal-key")
      node("_embedded.keys[1].translations.de.aiReview").isNull()
      node("_embedded.keys[0].keyName").isEqualTo("checkout-key")
      node("_embedded.keys[0].translations.de.aiReview").isNull()
    }
  }

  @Test
  fun `omits the summary when the feature flag is off`() {
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    getTranslations().andIsOk.andAssertThatJson {
      node("_embedded.keys[3].translations.de.aiReview").isNull()
    }
  }
}
