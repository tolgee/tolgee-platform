package io.tolgee.hateoas.machineTranslation

import io.tolgee.constants.MtServiceType

class StreamedSuggestionInfo(
  val servicesTypes: List<MtServiceType>,
  val promptId: Long?,
  val baseBlank: Boolean,
)
