package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewRuleControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  lateinit var testData: AiReviewTestData

  private val rulesUrl get() = "/v2/projects/${testData.project.id}/ai-review-rules"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `creates a rule with defaults`() {
    performAuthPost(
      rulesUrl,
      mapOf("name" to "Checkout", "tagIn" to listOf("checkout")),
    ).andIsOk.andAssertThatJson {
      node("id").isNumber
      node("name").isEqualTo("Checkout")
      node("enabled").isEqualTo(true)
      node("tagIn").isArray.containsExactly("checkout")
      node("threshold").isEqualTo(85)
      node("never").isEqualTo(false)
      node("mode").isEqualTo("SCORE_ONLY")
      node("skipOpenQaIssues").isEqualTo(true)
      node("promptId").isNull()
      node("languageIds").isArray.isEmpty()
    }
  }

  @Test
  fun `a rule without a name is rejected`() {
    performAuthPost(rulesUrl, mapOf("tagIn" to listOf("checkout"))).andIsBadRequest
    performAuthPost(rulesUrl, mapOf("name" to "  ", "tagIn" to listOf("checkout"))).andIsBadRequest
  }

  @Test
  fun `creates a rule with languages, branches and a review prompt`() {
    val body =
      mapOf(
        "name" to "Checkout",
        "languageIds" to listOf(testData.german.id, testData.french.id),
        "branchIn" to listOf("feature/*"),
        "branchNotIn" to listOf("draft/*"),
        "threshold" to 92,
        "mode" to "ACTIVE",
        "promptId" to testData.reviewPrompt.id,
      )
    performAuthPost(rulesUrl, body).andIsOk.andAssertThatJson {
      node("name").isEqualTo("Checkout")
      node("languageIds").isArray.containsExactlyInAnyOrder(testData.german.id, testData.french.id)
      node("branchIn").isArray.containsExactly("feature/*")
      node("branchNotIn").isArray.containsExactly("draft/*")
      node("threshold").isEqualTo(92)
      node("mode").isEqualTo("ACTIVE")
      node("promptId").isEqualTo(testData.reviewPrompt.id)
    }
  }

  @Test
  fun `rejects threshold 101`() {
    performAuthPost(rulesUrl, mapOf("threshold" to 101)).andIsBadRequest
  }

  @Test
  fun `rejects a TRANSLATE prompt`() {
    performAuthPost(rulesUrl, mapOf("name" to "R", "promptId" to testData.translatePrompt.id))
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_PROMPT_MUST_BE_REVIEW_TYPE)
  }

  @Test
  fun `rejects a language of another project`() {
    performAuthPost(rulesUrl, mapOf("name" to "R", "languageIds" to listOf(testData.otherProjectLanguage.id)))
      .andIsBadRequest
      .andHasErrorMessage(Message.LANGUAGE_NOT_FROM_PROJECT)
  }

  @Test
  fun `a rule of another project is not found`() {
    val otherRuleUrl = "$rulesUrl/${testData.otherProjectRule.id}"
    val body = mapOf("name" to "R", "tagIn" to listOf("ui"))
    performAuthGet(otherRuleUrl).andIsNotFound
    performAuthPut(otherRuleUrl, body).andIsNotFound
    performAuthPut("$otherRuleUrl/enabled", mapOf("enabled" to false)).andIsNotFound
    performAuthDelete(otherRuleUrl).andIsNotFound
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository
        .findById(testData.otherProjectRule.id)
        .get()
        .enabled.assert
        .isTrue()
    }
  }

  @Test
  fun `rejects a review prompt of another project`() {
    performAuthPost(rulesUrl, mapOf("name" to "R", "promptId" to testData.otherProjectReviewPrompt.id))
      .andIsNotFound
    performAuthPut(
      "$rulesUrl/${testData.rule.id}",
      mapOf("name" to "R", "promptId" to testData.otherProjectReviewPrompt.id),
    ).andIsNotFound
  }

  @Test
  fun `rejects the base language`() {
    performAuthPost(rulesUrl, mapOf("name" to "R", "languageIds" to listOf(testData.englishLanguage.id)))
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_RULE_BASE_LANGUAGE)
  }

  @Test
  fun `rejects a branch pattern with more than 5 wildcards`() {
    performAuthPost(rulesUrl, mapOf("name" to "R", "branchIn" to listOf("*a*b*c*d*e*")))
      .andIsBadRequest
      .andHasErrorMessage(Message.FILTER_PATTERN_NOT_VALID)
  }

  @Test
  fun `updates the scope`() {
    performAuthPut(
      "$rulesUrl/${testData.rule.id}",
      mapOf(
        "name" to "Scoped",
        "tagIn" to listOf("legal"),
      ),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("Scoped")
      node("tagIn").isArray.containsExactly("legal")
    }
  }

  @Test
  fun `lists and gets rules`() {
    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules").isArray.hasSize(1)
      node("_embedded.aiReviewRules[0].name").isEqualTo("UI")
    }
    performAuthGet("$rulesUrl/${testData.rule.id}").andIsOk.andAssertThatJson {
      node("name").isEqualTo("UI")
    }
  }

  @Test
  fun `toggles enabled`() {
    performAuthPut("$rulesUrl/${testData.rule.id}/enabled", mapOf("enabled" to false)).andIsOk.andAssertThatJson {
      node("enabled").isEqualTo(false)
    }
  }

  @Test
  fun `deletes a rule`() {
    performAuthDelete("$rulesUrl/${testData.rule.id}").andIsOk
    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository
        .findById(testData.rule.id)
        .isPresent.assert
        .isFalse()
    }
  }

  @Test
  fun `view needs PROMPTS_VIEW and edit needs PROMPTS_EDIT`() {
    userAccount = testData.translator
    performAuthGet(rulesUrl).andIsForbidden

    userAccount = testData.promptsViewer
    performAuthGet(rulesUrl).andIsOk
    performAuthPost(rulesUrl, emptyMap<String, Any>()).andIsForbidden
  }
}
