package io.tolgee.service.machineTranslation

import io.tolgee.constants.MtServiceType
import io.tolgee.model.mtServiceConfig.Formality

data class MtServiceInfo(
  val serviceType: MtServiceType,
  val formality: Formality? = null,
  val promptId: Long? = null,
)
