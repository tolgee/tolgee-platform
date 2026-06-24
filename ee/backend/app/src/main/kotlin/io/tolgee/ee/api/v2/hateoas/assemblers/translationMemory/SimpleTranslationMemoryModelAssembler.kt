package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.SharedTranslationMemoryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.SimpleTranslationMemoryModel
import io.tolgee.model.translationMemory.TranslationMemory
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleTranslationMemoryModelAssembler :
  RepresentationModelAssemblerSupport<TranslationMemory, SimpleTranslationMemoryModel>(
    SharedTranslationMemoryController::class.java,
    SimpleTranslationMemoryModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemory): SimpleTranslationMemoryModel {
    return SimpleTranslationMemoryModel(
      id = entity.id,
      name = entity.name,
      sourceLanguageTag = entity.sourceLanguageTag,
      type = entity.type,
    )
  }
}
