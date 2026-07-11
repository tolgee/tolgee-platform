package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.SharedTranslationMemoryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryModel
import io.tolgee.hateoas.organization.SimpleOrganizationModelAssembler
import io.tolgee.model.translationMemory.TranslationMemory
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryModelAssembler(
  private val simpleOrganizationModelAssembler: SimpleOrganizationModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationMemory, TranslationMemoryModel>(
    SharedTranslationMemoryController::class.java,
    TranslationMemoryModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemory): TranslationMemoryModel {
    return TranslationMemoryModel(
      id = entity.id,
      name = entity.name,
      sourceLanguageTag = entity.sourceLanguageTag,
      type = entity.type,
      organizationOwner = simpleOrganizationModelAssembler.toModel(entity.organizationOwner),
      defaultPenalty = entity.defaultPenalty,
      writeOnlyReviewed = entity.writeOnlyReviewed,
    )
  }
}
