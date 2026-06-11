package io.tolgee.service.aiMatchStats

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import java.util.Date

class AiTextReconstructorTest {
  private fun ai(
    ts: Long,
    text: String?,
    promptId: Long? = null,
    mtProvider: String? = "PROMPT",
    state: String? = null,
  ) = AiMatchActivityRow(
    timestamp = Date(ts),
    textModified = text != null,
    textNew = text,
    stateNew = state,
    autoNew = true,
    mtProviderNew = mtProvider,
    promptIdNew = promptId,
  )

  private fun human(
    ts: Long,
    text: String? = null,
    state: String? = null,
  ) = AiMatchActivityRow(
    timestamp = Date(ts),
    textModified = text != null,
    textNew = text,
    stateNew = state,
    autoNew = false,
    mtProviderNew = null,
    promptIdNew = null,
  )

  @Test
  fun `snapshots the AI text seen at review`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          ai(10, "ai output", promptId = 5),
          human(20, state = "REVIEWED"),
        ),
      )
    result.aiText.assert.isEqualTo("ai output")
    result.sawReview.assert.isTrue()
    result.reviewedAt.assert.isEqualTo(Date(20))
    result.aiSource!!
      .promptId.assert
      .isEqualTo(5L)
  }

  @Test
  fun `keeps the AI text even after a reviewer edits before reviewing`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          ai(10, "ai output", promptId = 5),
          human(20, text = "human output"),
          human(30, state = "REVIEWED"),
        ),
      )
    result.aiText.assert.isEqualTo("ai output")
    result.aiSource!!
      .promptId.assert
      .isEqualTo(5L)
  }

  @Test
  fun `last review wins when re-translated and re-reviewed`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          ai(10, "v1", promptId = 5),
          human(20, state = "REVIEWED"),
          ai(30, "v2", promptId = 7),
          human(40, state = "REVIEWED"),
        ),
      )
    result.aiText.assert.isEqualTo("v2")
    result.reviewedAt.assert.isEqualTo(Date(40))
    result.aiSource!!
      .promptId.assert
      .isEqualTo(7L)
  }

  @Test
  fun `falls back to last AI text and last timestamp when never reviewed`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          ai(10, "x", promptId = 5),
        ),
      )
    result.sawReview.assert.isFalse()
    result.aiText.assert.isEqualTo("x")
    result.reviewedAt.assert.isEqualTo(Date(10))
  }

  @Test
  fun `returns null AI text when no AI revision exists`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          human(10, text = "purely human"),
          human(20, state = "REVIEWED"),
        ),
      )
    result.aiText.assert.isNull()
    result.aiSource.assert.isNull()
    result.sawReview.assert.isTrue()
  }

  @Test
  fun `setting text to null does not overwrite the reconstructed current text`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          ai(10, "ai", promptId = 5),
          human(20, text = null),
          human(30, state = "REVIEWED"),
        ),
      )
    result.aiText.assert.isEqualTo("ai")
  }

  @Test
  fun `MT-engine revision is recognized as AI even without a prompt id`() {
    val result =
      AiTextReconstructor.walk(
        listOf(
          ai(10, "google text", promptId = null, mtProvider = "GOOGLE"),
          human(20, state = "REVIEWED"),
        ),
      )
    result.aiText.assert.isEqualTo("google text")
    result.aiSource!!
      .mtProvider.assert
      .isEqualTo("GOOGLE")
    result.aiSource!!
      .promptId.assert
      .isNull()
  }
}
