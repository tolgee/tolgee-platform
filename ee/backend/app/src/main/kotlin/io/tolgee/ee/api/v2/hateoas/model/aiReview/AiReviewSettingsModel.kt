package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.tolgee.model.aiReview.AiReviewProjectSettings
import io.tolgee.model.enums.aiReview.SourceChangeAction

data class AiReviewSettingsModel(
  val onSourceChangeAiApproved: SourceChangeAction,
  val onSourceChangeHumanEdited: SourceChangeAction,
  val onSourceChangeHumanReviewed: SourceChangeAction,
) {
  companion object {
    fun of(settings: AiReviewProjectSettings) =
      AiReviewSettingsModel(
        onSourceChangeAiApproved = settings.onSourceChangeAiApproved,
        onSourceChangeHumanEdited = settings.onSourceChangeHumanEdited,
        onSourceChangeHumanReviewed = settings.onSourceChangeHumanReviewed,
      )
  }
}
