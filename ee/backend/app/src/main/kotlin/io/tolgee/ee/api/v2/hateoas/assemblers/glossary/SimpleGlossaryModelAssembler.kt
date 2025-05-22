package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryController
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryModel
import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.glossary.Glossary
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleGlossaryModelAssembler(
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
) : RepresentationModelAssemblerSupport<Glossary, SimpleGlossaryModel>(
    GlossaryController::class.java,
    SimpleGlossaryModel::class.java,
  ) {
  override fun toModel(entity: Glossary): SimpleGlossaryModel =
    SimpleGlossaryModel(
      id = entity.id,
      name = entity.name,
      baseLanguageTag = entity.baseLanguageTag,
      assignedProjects = simpleProjectModelAssembler.toCollectionModel(entity.assignedProjects),
    )
}
