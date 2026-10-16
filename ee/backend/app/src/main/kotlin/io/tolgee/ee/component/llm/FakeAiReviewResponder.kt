package io.tolgee.ee.component.llm

import io.tolgee.ee.service.prompt.PromptFragmentsHelper
import io.tolgee.model.enums.aiReview.AiReviewIssueSeverity
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class FakeAiReviewResponder(
  private val objectMapper: ObjectMapper,
) {
  fun isReviewPrompt(promptText: String): Boolean = promptText.contains(PromptFragmentsHelper.REVIEW_JSON_MARK)

  fun respond(promptText: String): String {
    val reviewedText =
      promptText
        .substringAfterLast(PromptFragmentsHelper.REVIEW_TARGET_MARK)
        .substringAfter(REVIEWED_TEXT_DELIMITER)
        .substringBefore(REVIEWED_TEXT_DELIMITER)
    if (reviewedText.contains(FAKE_AI_REVIEW_INVALID)) {
      return "not a json"
    }
    val issues =
      FAKE_AI_REVIEW_SEVERITIES
        .filter { (token, _) -> reviewedText.contains(token) }
        .map { (_, severity) ->
          mapOf(
            "severity" to severity.name.lowercase(),
            "category" to "fake",
            "message" to "Fake ${severity.name.lowercase()} issue",
            "points" to severity.minPoints,
          )
        }
    return objectMapper.writeValueAsString(mapOf("issues" to issues))
  }

  companion object {
    private const val REVIEWED_TEXT_DELIMITER = "\"\"\""
    const val FAKE_AI_REVIEW_INVALID = "FAKE_AI_REVIEW_INVALID"
    val FAKE_AI_REVIEW_SEVERITIES =
      listOf(
        "FAKE_AI_REVIEW_CRITICAL" to AiReviewIssueSeverity.CRITICAL,
        "FAKE_AI_REVIEW_MAJOR" to AiReviewIssueSeverity.MAJOR,
        "FAKE_AI_REVIEW_MINOR" to AiReviewIssueSeverity.MINOR,
      )
  }
}
