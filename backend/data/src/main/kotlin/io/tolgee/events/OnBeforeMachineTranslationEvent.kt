package io.tolgee.events

import io.tolgee.model.Project

class OnBeforeMachineTranslationEvent(
  source: Any,
  textToTranslate: String,
  project: Project,
  expectedSumPrice: Int
) : MachineTranslationEvent(source, textToTranslate, project, expectedSumPrice)
