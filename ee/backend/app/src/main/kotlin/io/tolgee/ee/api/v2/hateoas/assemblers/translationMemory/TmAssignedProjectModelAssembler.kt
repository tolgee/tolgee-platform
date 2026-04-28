package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.SharedTranslationMemoryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TmAssignedProjectModel
import io.tolgee.model.translationMemory.TranslationMemoryProject
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TmAssignedProjectModelAssembler :
  RepresentationModelAssemblerSupport<TranslationMemoryProject, TmAssignedProjectModel>(
    SharedTranslationMemoryController::class.java,
    TmAssignedProjectModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryProject): TmAssignedProjectModel {
    return TmAssignedProjectModel(
      projectId = entity.project.id,
      projectName = entity.project.name,
      readAccess = entity.readAccess,
      writeAccess = entity.writeAccess,
      priority = entity.priority,
      penalty = entity.penalty,
    )
  }
}
