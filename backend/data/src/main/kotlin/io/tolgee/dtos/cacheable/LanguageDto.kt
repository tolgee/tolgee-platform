package io.tolgee.dtos.cacheable

import io.tolgee.model.ILanguage

data class LanguageDto(
  override var id: Long = 0,
  override var name: String = "",
  override var tag: String = "",
  override var originalName: String? = null,
  override var flagEmoji: String? = null,
  override var aiTranslatorPromptDescription: String? = null,
  var base: Boolean = false,
) : ILanguage {
  companion object {
    fun fromEntity(
      language: ILanguage,
      baseLanguageId: Long?,
    ): LanguageDto {
      return LanguageDto(
        id = language.id,
        name = language.name,
        tag = language.tag,
        originalName = language.originalName,
        flagEmoji = language.flagEmoji,
        aiTranslatorPromptDescription = language.aiTranslatorPromptDescription,
        base = baseLanguageId == language.id,
      )
    }
  }
}
