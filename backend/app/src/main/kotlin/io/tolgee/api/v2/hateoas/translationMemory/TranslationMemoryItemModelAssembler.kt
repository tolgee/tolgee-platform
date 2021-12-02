package io.tolgee.api.v2.hateoas.invitation

import io.tolgee.api.v2.controllers.TagController
import io.tolgee.api.v2.hateoas.translationMemory.TranslationMemoryItemModel
import io.tolgee.dtos.TranslationMemoryItem
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryItemModelAssembler : RepresentationModelAssemblerSupport<TranslationMemoryItem, TranslationMemoryItemModel>(
  TagController::class.java, TranslationMemoryItemModel::class.java
) {
  override fun toModel(entity: TranslationMemoryItem): TranslationMemoryItemModel {
    return TranslationMemoryItemModel(
      targetText = entity.targetText,
      baseText = entity.baseText,
      keyName = entity.keyName,
      match = entity.match
    )
  }
}
