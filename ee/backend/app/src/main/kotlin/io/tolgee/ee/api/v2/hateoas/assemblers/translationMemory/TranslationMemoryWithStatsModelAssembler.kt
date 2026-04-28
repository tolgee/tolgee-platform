package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.SharedTranslationMemoryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryWithStatsModel
import io.tolgee.model.translationMemory.TranslationMemoryWithStats
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryWithStatsModelAssembler :
  RepresentationModelAssemblerSupport<TranslationMemoryWithStats, TranslationMemoryWithStatsModel>(
    SharedTranslationMemoryController::class.java,
    TranslationMemoryWithStatsModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryWithStats): TranslationMemoryWithStatsModel =
    TranslationMemoryWithStatsModel(
      id = entity.id,
      name = entity.name,
      sourceLanguageTag = entity.sourceLanguageTag,
      type = entity.type,
      entryCount = entity.entryCount,
      assignedProjectsCount = entity.assignedProjectsCount,
      assignedProjectNames =
        entity.assignedProjectNames
          ?.split(",")
          ?.filter { it.isNotBlank() }
          ?: emptyList(),
      defaultPenalty = entity.defaultPenalty,
      writeOnlyReviewed = entity.writeOnlyReviewed,
    )
}
