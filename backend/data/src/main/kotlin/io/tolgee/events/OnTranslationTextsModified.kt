package io.tolgee.events

import org.springframework.context.ApplicationEvent

/**
 * Dispatched when translation texts are modified through paths that don't publish [OnTranslationsSet]
 * (batch operations, auto-translate, demo project creation, key import).
 */
class OnTranslationTextsModified(
  source: Any,
  val translationIds: List<Long>,
  val projectId: Long,
) : ApplicationEvent(source)
