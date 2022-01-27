package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

data class AutoTranslationSettingsDto(
  @Schema(
    description = "If true, new keys will be automatically translated using translation memory" +
      " when 100% match is found"
  )
  var usingTranslationMemory: Boolean = false,
  @Schema(
    description = "If true, new keys will be automatically translated " +
      "using primary machine translation service." +
      "" +
      "When \"usingTranslationMemory\" is enabled, it tries to translate it with translation memory first."
  )
  var usingMachineTranslation: Boolean = false
)
