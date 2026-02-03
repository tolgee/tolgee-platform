package io.tolgee.dtos.request

data class SetMachineTranslationSettingsDto(
  var settings: List<MachineTranslationLanguagePropsDto> = listOf(),
)
