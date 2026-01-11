package io.tolgee.events

import org.springframework.context.ApplicationEvent

class OnBeforeMachineTranslationEvent(
  source: Any,
  val organizationId: Long,
) : ApplicationEvent(source)
