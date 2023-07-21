package io.tolgee.events

import io.tolgee.model.Project
import org.springframework.context.ApplicationEvent

class OnBeforeMachineTranslationEvent(
  source: Any,
  val project: Project,
) : ApplicationEvent(source)
