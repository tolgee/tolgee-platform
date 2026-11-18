package io.tolgee.service.aiReview

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.enums.SourceChangeAction

data class EffectiveSourceChangeSettings(
  val aiApproved: SourceChangeAction,
  val humanEdited: SourceChangeAction,
  val humanReviewed: SourceChangeAction,
) {
  companion object {
    val DEFAULTS =
      EffectiveSourceChangeSettings(
        aiApproved = SourceChangeAction.RETRANSLATE,
        humanEdited = SourceChangeAction.MARK_OUTDATED,
        humanReviewed = SourceChangeAction.MARK_OUTDATED,
      )
  }
}

interface SourceChangeSettingsProvider {
  fun getEffective(project: ProjectDto): EffectiveSourceChangeSettings
}
