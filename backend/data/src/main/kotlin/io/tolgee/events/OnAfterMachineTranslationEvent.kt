package io.tolgee.events

import io.tolgee.model.Project
import org.springframework.context.ApplicationEvent

class OnAfterMachineTranslationEvent(
  source: Any,
  /**
   * The project containing translation
   */
  val project: Project,

  /**
   * The actual total price of translation actually consumed
   */
  val actualSumPrice: Int
) : ApplicationEvent(source)
