package io.tolgee.ee.component.slackIntegration.data

import io.tolgee.model.enums.TranslationState

/**
 * Slack specific view of [io.tolgee.model.translation.Translation]
 */
data class SlackTranslationInfoDto(
  val keyId: Long,
  val translationId: Long,
  val languageTag: String,
  val languageId: Long,
  val languageName: String,
  val languageFlagEmoji: String?,
  val text: String?,
  val state: TranslationState,
)
