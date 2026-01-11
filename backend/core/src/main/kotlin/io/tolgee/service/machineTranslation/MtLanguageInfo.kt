package io.tolgee.service.machineTranslation

import io.tolgee.dtos.cacheable.LanguageDto

data class MtLanguageInfo(
  val language: LanguageDto?,
  val supportedServices: List<MtSupportedService>,
)
