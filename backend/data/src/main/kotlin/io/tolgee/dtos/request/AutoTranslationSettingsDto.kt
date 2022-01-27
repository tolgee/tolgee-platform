package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

data class AutoTranslationSettingsDto(
  @Schema(
    description = "If true, new keys will be automatically translated using translation memory" +
      " when 100% match is found"
  )
  val usingTranslationMemory: Boolean,
  @Schema(
    description = "If true, new keys will be automatically translated " +
      "using primary machine translation service." +
      "" +
      "When \"usingTm\" is enabled, it tries to translate it with translation memory first."
  )
  val usingPrimaryMachineTranslationService: Boolean
)
