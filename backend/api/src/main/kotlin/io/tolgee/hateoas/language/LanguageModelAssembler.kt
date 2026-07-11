package io.tolgee.hateoas.language

import io.tolgee.api.v2.controllers.V2LanguagesController
import io.tolgee.dtos.cacheable.LanguageDto
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LanguageModelAssembler :
  RepresentationModelAssemblerSupport<LanguageDto, LanguageModel>(
    V2LanguagesController::class.java,
    LanguageModel::class.java,
  ) {
  override fun toModel(languageDto: LanguageDto): LanguageModel {
    return LanguageModel(
      id = languageDto.id,
      name = languageDto.name,
      originalName = languageDto.originalName,
      tag = languageDto.tag,
      flagEmoji = languageDto.flagEmoji,
      base = languageDto.base,
    )
  }
}
