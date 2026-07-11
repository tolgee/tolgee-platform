package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryController
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryWithStatsModel
import io.tolgee.ee.data.glossary.GlossaryWithStats
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleGlossaryWithStatsModelAssembler :
  RepresentationModelAssemblerSupport<GlossaryWithStats, SimpleGlossaryWithStatsModel>(
    GlossaryController::class.java,
    SimpleGlossaryWithStatsModel::class.java,
  ) {
  override fun toModel(entity: GlossaryWithStats): SimpleGlossaryWithStatsModel =
    SimpleGlossaryWithStatsModel(
      id = entity.id,
      name = entity.name,
      baseLanguageTag = entity.baseLanguageTag,
      firstAssignedProjectName = entity.firstAssignedProjectName,
      assignedProjectsCount = entity.assignedProjectsCount,
    )
}
