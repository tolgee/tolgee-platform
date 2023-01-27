package io.tolgee.api.v2.hateoas.language

import io.tolgee.api.v2.controllers.V2LanguagesController
import io.tolgee.model.Language
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LanguageModelAssembler : RepresentationModelAssemblerSupport<Language, LanguageModel>(
  V2LanguagesController::class.java, LanguageModel::class.java
) {
  override fun toModel(entity: Language): LanguageModel {
    return LanguageModel(
      id = entity.id,
      name = entity.name ?: "",
      originalName = entity.originalName,
      tag = entity.tag,
      flagEmoji = entity.flagEmoji,
      base = entity.project.baseLanguage?.id == entity.id
    )
  }
}
