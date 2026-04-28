package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.ProjectTranslationMemoryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.ProjectTranslationMemoryAssignmentModel
import io.tolgee.model.translationMemory.TranslationMemoryProject
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class ProjectTranslationMemoryAssignmentModelAssembler :
  RepresentationModelAssemblerSupport<TranslationMemoryProject, ProjectTranslationMemoryAssignmentModel>(
    ProjectTranslationMemoryController::class.java,
    ProjectTranslationMemoryAssignmentModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryProject): ProjectTranslationMemoryAssignmentModel {
    return ProjectTranslationMemoryAssignmentModel(
      translationMemoryId = entity.translationMemory.id,
      translationMemoryName = entity.translationMemory.name,
      sourceLanguageTag = entity.translationMemory.sourceLanguageTag,
      type = entity.translationMemory.type,
      readAccess = entity.readAccess,
      writeAccess = entity.writeAccess,
      priority = entity.priority,
      defaultPenalty = entity.translationMemory.defaultPenalty,
      penalty = entity.penalty,
      writeOnlyReviewed = entity.translationMemory.writeOnlyReviewed,
    )
  }
}
