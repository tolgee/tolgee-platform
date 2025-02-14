package io.tolgee.ee.component.slackIntegration.data

import io.tolgee.model.enums.TranslationState

data class TranslationInfoDto(
  val keyId: Long,
  val translationId: Long,
  val languageTag: String,
  val languageId: Long,
  val languageName: String,
  val languageFlagEmoji: String?,
  val text: String?,
  val state: TranslationState,
)
