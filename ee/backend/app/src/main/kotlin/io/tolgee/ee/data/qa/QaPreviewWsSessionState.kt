package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckType
import kotlinx.coroutines.Job
import java.time.Instant

data class QaPreviewWsSessionState(
  val projectId: Long,
  val baseText: String?,
  val baseLanguageTag: String?,
  val languageTag: String,
  val keyId: Long?,
  val translationId: Long?,
  val enabledCheckTypes: List<QaCheckType>,
  val isPlural: Boolean = false,
  val baseVariants: Map<String, String>? = null,
  val maxCharLimit: Int? = null,
  val icuPlaceholders: Boolean = true,
) {
  var currentJob: Job? = null
  var lastMessageTime: Instant? = null
  var messageCount: Int = 0
}
