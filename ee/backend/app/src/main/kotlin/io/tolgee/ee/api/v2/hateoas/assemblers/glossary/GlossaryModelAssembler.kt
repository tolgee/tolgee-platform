package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryController
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryModel
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.glossary.Glossary
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class GlossaryModelAssembler(
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
) : RepresentationModelAssemblerSupport<Glossary, GlossaryModel>(
    GlossaryController::class.java,
    GlossaryModel::class.java,
  ) {
  override fun toModel(entity: Glossary): GlossaryModel {
    return GlossaryModel(
      id = entity.id,
      name = entity.name,
      baseLanguageCode = entity.baseLanguageCode,
      assignedProjects = simpleProjectModelAssembler.toCollectionModel(entity.assignedProjects),
    )
  }
}
