package io.tolgee.service.machineTranslation

import io.tolgee.model.Language

data class MtLanguageInfo(
  val language: Language,
  val supportedServices: List<MtSupportedService>,
)
