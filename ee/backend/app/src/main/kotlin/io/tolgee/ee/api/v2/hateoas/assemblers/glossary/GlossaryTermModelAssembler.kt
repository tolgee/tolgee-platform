package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryTermController
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermModel
import io.tolgee.model.glossary.GlossaryTerm
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class GlossaryTermModelAssembler(
  private val glossaryModelAssembler: GlossaryModelAssembler,
) : RepresentationModelAssemblerSupport<GlossaryTerm, GlossaryTermModel>(
    GlossaryTermController::class.java,
    GlossaryTermModel::class.java,
  ) {
  override fun toModel(entity: GlossaryTerm): GlossaryTermModel {
    return GlossaryTermModel(
      id = entity.id,
      glossary = glossaryModelAssembler.toModel(entity.glossary),
      description = entity.description,
      flagNonTranslatable = entity.flagNonTranslatable,
      flagCaseSensitive = entity.flagCaseSensitive,
      flagAbbreviation = entity.flagAbbreviation,
      flagForbiddenTerm = entity.flagForbiddenTerm,
    )
  }
}
