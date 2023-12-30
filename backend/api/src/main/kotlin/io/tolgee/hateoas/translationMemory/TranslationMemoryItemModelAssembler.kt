package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.controllers.TagsController
import io.tolgee.hateoas.translationMemory.TranslationMemoryItemModel
import io.tolgee.model.views.TranslationMemoryItemView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryItemModelAssembler :
  RepresentationModelAssemblerSupport<TranslationMemoryItemView, TranslationMemoryItemModel>(
    TagsController::class.java,
    TranslationMemoryItemModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryItemView): TranslationMemoryItemModel {
    return TranslationMemoryItemModel(
      targetText = entity.targetTranslationText,
      baseText = entity.baseTranslationText,
      keyName = entity.keyName,
      similarity = entity.similarity,
    )
  }
}
