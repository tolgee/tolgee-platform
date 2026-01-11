package io.tolgee.events

import io.tolgee.model.Language
import org.springframework.context.ApplicationEvent

class OnLanguagePrePersist(
  source: Any,
  val language: Language,
) : ApplicationEvent(source)
