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
  val isPlural: Boolean,
  val baseVariants: Map<String, String>?,
  val maxCharLimit: Int?,
  val icuPlaceholders: Boolean,
  val organizationOwnerId: Long,
  val glossaryEnabled: Boolean,
) {
  // Accessed from both the WS handler thread and afterConnectionClosed — must be volatile
  @Volatile
  var currentJob: Job? = null
  var lastMessageTime: Instant? = null
  var messageCount: Int = 0
}
