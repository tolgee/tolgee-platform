package io.tolgee.ee.data.aiReview

import io.tolgee.model.enums.SourceChangeAction

data class SourceChangeSettingsRequest(
  val aiApproved: SourceChangeAction,
  val humanEdited: SourceChangeAction,
  val humanReviewed: SourceChangeAction,
)
