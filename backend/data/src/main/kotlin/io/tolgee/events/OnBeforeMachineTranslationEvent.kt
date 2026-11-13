package io.tolgee.events

import io.tolgee.constants.MtServiceType
import org.springframework.context.ApplicationEvent

class OnBeforeMachineTranslationEvent(
  source: Any,
  val organizationId: Long,
  val services: Set<MtServiceType>,
  val llmProviders: Set<String>,
) : ApplicationEvent(source)
