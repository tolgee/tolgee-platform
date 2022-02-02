package io.tolgee.events

import io.tolgee.model.Project

class OnAfterMachineTranslationEvent(
  source: Any,
  /**
   * The text which is going to be translated
   */
  textToTranslate: String,

  /**
   * The project containing translation
   */
  project: Project,

  /**
   * The total price of translation in credits, which was expected
   */
  expectedSumPrice: Int,

  /**
   * The actual total price of translation actually consumed
   */
  val actualSumPrice: Int
) : MachineTranslationEvent(source, textToTranslate, project, expectedSumPrice)
