package io.tolgee.ee.aiReview

import io.tolgee.ee.data.aiReview.AiReviewSettingsRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.aiReview.SourceChangeAction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiReviewSettingsControllerTest : AbstractAiReviewTest() {
  private val url get() = "/v2/projects/$projectId/ai-review/settings"

  @BeforeEach
  fun save() {
    saveTestData()
  }

  @Test
  fun `returns the defaults`() {
    performAuthGet(url).andIsOk.andAssertThatJson {
      node("onSourceChangeAiApproved").isEqualTo("RETRANSLATE")
      node("onSourceChangeHumanEdited").isEqualTo("KEEP")
      node("onSourceChangeHumanReviewed").isEqualTo("KEEP")
    }
  }

  @Test
  fun `updates the settings`() {
    val request =
      AiReviewSettingsRequest(
        onSourceChangeAiApproved = SourceChangeAction.KEEP,
        onSourceChangeHumanEdited = SourceChangeAction.RETRANSLATE,
        onSourceChangeHumanReviewed = SourceChangeAction.RETRANSLATE,
      )
    performAuthPut(url, request).andIsOk
    performAuthPut(url, request).andIsOk

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("onSourceChangeAiApproved").isEqualTo("KEEP")
      node("onSourceChangeHumanEdited").isEqualTo("RETRANSLATE")
      node("onSourceChangeHumanReviewed").isEqualTo("RETRANSLATE")
    }
  }

  @Test
  fun `needs the permission to edit prompts`() {
    userAccount = testData.promptViewer
    performAuthGet(url).andIsOk
    performAuthPut(url, AiReviewSettingsRequest()).andIsForbidden
  }

  @Test
  fun `is hidden while the feature flag is off`() {
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    performAuthGet(url).andIsNotFound
  }
}
