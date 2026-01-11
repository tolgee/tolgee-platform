package io.tolgee.service.machineTranslation

import io.tolgee.constants.MtServiceType

data class MtSupportedService(
  val serviceType: MtServiceType,
  val formalitySupported: Boolean,
)
