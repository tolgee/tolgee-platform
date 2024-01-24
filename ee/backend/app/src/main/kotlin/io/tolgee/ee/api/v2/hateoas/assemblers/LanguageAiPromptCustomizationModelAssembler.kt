package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.hateoas.aiPtomptCustomization.LanguageAiPromptCustomizationModel
import io.tolgee.hateoas.language.LanguageModelAssembler
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class LanguageAiPromptCustomizationModelAssembler(
  private val languageModelAssembler: LanguageModelAssembler,
) : RepresentationModelAssembler<LanguageDto, LanguageAiPromptCustomizationModel> {
  override fun toModel(dto: LanguageDto): LanguageAiPromptCustomizationModel {
    return LanguageAiPromptCustomizationModel(
      language = languageModelAssembler.toModel(dto),
      description = dto.aiTranslatorPromptDescription,
    )
  }
}
