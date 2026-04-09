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
  @Volatile
  var currentJob: Job? = null

  @Volatile
  var lastMessageTime: Instant? = null

  @Volatile
  var messageCount: Int = 0

  @Synchronized
  fun cancelAndSetJob(newJobAction: () -> Job) {
    currentJob?.cancel()
    currentJob = newJobAction()
  }

  @Synchronized
  fun cancelJob() {
    currentJob?.cancel()
    currentJob = null
  }

  @Synchronized
  fun tryAcceptMessage(): Boolean {
    val now = Instant.now()
    val lastMessage = lastMessageTime

    if (lastMessage != null && java.time.Duration
        .between(lastMessage, now)
        .toMillis() < RATE_LIMIT_WINDOW_MS
    ) {
      if (messageCount >= MAX_MESSAGES_PER_WINDOW) {
        return false
      }
      messageCount++
    } else {
      messageCount = 1
    }
    lastMessageTime = now
    return true
  }

  companion object {
    const val RATE_LIMIT_WINDOW_MS = 1000L
    const val MAX_MESSAGES_PER_WINDOW = 20
  }
}
