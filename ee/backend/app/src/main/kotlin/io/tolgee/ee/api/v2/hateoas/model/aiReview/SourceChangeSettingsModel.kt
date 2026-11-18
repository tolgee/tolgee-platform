package io.tolgee.ee.api.v2.hateoas.model.aiReview

import io.tolgee.model.enums.SourceChangeAction

data class SourceChangeSettingsModel(
  val aiApproved: SourceChangeAction,
  val humanEdited: SourceChangeAction,
  val humanReviewed: SourceChangeAction,
  val aiApprovedLocked: Boolean,
  val humanReviewedLocked: Boolean,
)
