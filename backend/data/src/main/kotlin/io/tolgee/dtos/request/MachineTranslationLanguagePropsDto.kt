package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType

data class MachineTranslationLanguagePropsDto(
  @Schema(description = "The language to apply those rules. If null, then this settings are default.")
  var targetLanguageId: Long? = null,

  @Schema(description = "This service will be used for automated translation")
  var primaryService: MtServiceType? = null,

  @Schema(description = "List of enabled services")
  var enabledServices: Set<MtServiceType> = setOf()
)
