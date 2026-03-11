package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckType
import kotlinx.coroutines.Job

data class QaPreviewWsSessionState(
  val projectId: Long,
  val baseText: String?,
  val baseLanguageTag: String?,
  val languageTag: String,
  val keyId: Long?,
  val translationId: Long?,
  val enabledCheckTypes: List<QaCheckType>,
  var currentJob: Job? = null,
)
