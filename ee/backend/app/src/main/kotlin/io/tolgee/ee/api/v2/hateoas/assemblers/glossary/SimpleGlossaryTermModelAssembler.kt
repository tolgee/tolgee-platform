package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryTermController
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryTermModel
import io.tolgee.model.glossary.GlossaryTerm
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class SimpleGlossaryTermModelAssembler :
  RepresentationModelAssemblerSupport<GlossaryTerm, SimpleGlossaryTermModel>(
    GlossaryTermController::class.java,
    SimpleGlossaryTermModel::class.java,
  ) {
  override fun toModel(entity: GlossaryTerm): SimpleGlossaryTermModel {
    return SimpleGlossaryTermModel(
      id = entity.id,
      description = entity.description,
      flagNonTranslatable = entity.flagNonTranslatable,
      flagCaseSensitive = entity.flagCaseSensitive,
      flagAbbreviation = entity.flagAbbreviation,
      flagForbiddenTerm = entity.flagForbiddenTerm,
    )
  }
}
