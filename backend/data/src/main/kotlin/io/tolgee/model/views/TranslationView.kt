package io.tolgee.model.views

import io.tolgee.constants.MtServiceType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Label

data class TranslationView(
  val id: Long,
  val text: String?,
  val state: TranslationState,
  val auto: Boolean,
  val mtProvider: MtServiceType?,
  val commentCount: Long,
  val unresolvedCommentCount: Long,
  val outdated: Boolean,
) {
  lateinit var labels: List<Label>
}
