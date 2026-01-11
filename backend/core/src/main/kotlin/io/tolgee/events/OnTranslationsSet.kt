package io.tolgee.events

import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import org.springframework.context.ApplicationEvent

/**
 * This event is dispatched when base translation for some key provided.
 * It is dispatched even when new key is created with base translation provided
 */
class OnTranslationsSet(
  source: Any,
  val key: Key,
  /**
   * Map of old values languageTag -> String
   */
  val oldValues: Map<String, String?>,
  val translations: List<Translation>,
) : ApplicationEvent(source)
