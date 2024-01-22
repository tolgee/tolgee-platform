package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

data class AutoTranslationSettingsDto(
  val languageId: Long? = null,
  @Schema(
    description =
      "If true, new keys will be automatically translated via batch operation using translation memory" +
        " when 100% match is found",
  )
  var usingTranslationMemory: Boolean = false,
  @Schema(
    description =
      "If true, new keys will be automatically translated via batch operation" +
        "using primary machine translation service." +
        "" +
        "When \"usingTranslationMemory\" is enabled, it tries to translate it with translation memory first.",
  )
  var usingMachineTranslation: Boolean = false,
  @Schema(
    description =
      "If true, import will trigger batch operation to translate the new new keys." +
        "\n" +
        "It includes also the data imported via CLI, Figma, or other integrations using batch key import.",
  )
  var enableForImport: Boolean = false,
)
