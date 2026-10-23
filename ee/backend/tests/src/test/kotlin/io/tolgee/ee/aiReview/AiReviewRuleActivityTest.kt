package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewRuleActivityTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

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
  fun `creating a rule is logged`() {
    val body =
      mapOf(
        "name" to "Legal",
        "never" to true,
        "promptId" to testData.reviewPrompt.id,
        "languageIds" to listOf(testData.german.id),
        "overrides" to listOf(mapOf("languageId" to testData.german.id, "threshold" to 98)),
      )
    performAuthPost("$projectUrl/ai-review-rules", body).andIsOk

    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].type").isEqualTo("AI_REVIEW_RULE_CREATE")
      node("_embedded.activities[0].author.id").isEqualTo(testData.user.id)
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.name.new").isEqualTo("Legal")
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.never.new").isEqualTo(true)
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.prompt.new.entityId")
        .isEqualTo(testData.reviewPrompt.id)
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.prompt.new.data.name")
        .isEqualTo("Brand voice review")
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.languages.new")
        .isArray
        .containsExactly(testData.german.id)
      node("_embedded.activities[0].modifiedEntities.AiReviewRuleLanguageOverride[0].modifications.threshold.new")
        .isEqualTo(98)
    }
  }

  @Test
  fun `updating a rule is logged with old and new values`() {
    performAuthPut("$projectUrl/ai-review-rules/${testData.rule.id}", mapOf("threshold" to 60, "mode" to "ACTIVE"))
      .andIsOk

    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].type").isEqualTo("AI_REVIEW_RULE_UPDATE")
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.threshold.old").isEqualTo(85)
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.threshold.new").isEqualTo(60)
    }
  }

  @Test
  fun `enabling and disabling is logged as an update`() {
    performAuthPut("$projectUrl/ai-review-rules/${testData.rule.id}/enabled", mapOf("enabled" to false)).andIsOk

    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].type").isEqualTo("AI_REVIEW_RULE_UPDATE")
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.enabled.new").isEqualTo(false)
    }
  }

  @Test
  fun `deleting a rule is logged`() {
    performAuthDelete("$projectUrl/ai-review-rules/${testData.rule.id}").andIsOk

    performAuthGet("$projectUrl/activity").andIsOk.andAssertThatJson {
      node("_embedded.activities[0].type").isEqualTo("AI_REVIEW_RULE_DELETE")
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].entityId").isEqualTo(testData.rule.id)
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.name.old").isEqualTo("UI")
      node("_embedded.activities[0].modifiedEntities.AiReviewRule[0].modifications.name.new").isNull()
    }
  }
}
