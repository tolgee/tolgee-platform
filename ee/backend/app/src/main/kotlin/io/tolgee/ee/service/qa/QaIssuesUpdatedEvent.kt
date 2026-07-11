package io.tolgee.ee.service.qa

import io.tolgee.model.qa.TranslationQaIssue

data class QaIssuesUpdatedEvent(
  val translationId: Long,
  val keyId: Long,
  val languageTag: String,
  val qaChecksStale: Boolean,
  val issues: List<TranslationQaIssue>? = null,
)
