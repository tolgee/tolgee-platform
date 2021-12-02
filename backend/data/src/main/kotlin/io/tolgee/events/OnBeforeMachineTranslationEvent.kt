package io.tolgee.events

import io.tolgee.model.Project
import org.springframework.context.ApplicationEvent

class OnBeforeMachineTranslationEvent(
  source: Any,
  /**
   * The text which is going to be translated
   */
  val textToTranslate: String,

  /**
   * The project containing translation
   */
  val project: Project,

  /**
   * The total price of translation in credits
   */
  val sumPrice: Int
) : ApplicationEvent(source)
