package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryTermController
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermWithTranslationsModel
import io.tolgee.ee.data.glossary.GlossaryTermWithTranslationsView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class GlossaryTermWithTranslationsModelAssembler(
  private val glossaryTermTranslationModelAssembler: GlossaryTermTranslationModelAssembler,
) : RepresentationModelAssemblerSupport<GlossaryTermWithTranslationsView, GlossaryTermWithTranslationsModel>(
    GlossaryTermController::class.java,
    GlossaryTermWithTranslationsModel::class.java,
  ) {
  override fun toModel(entity: GlossaryTermWithTranslationsView): GlossaryTermWithTranslationsModel {
    return GlossaryTermWithTranslationsModel(
      id = entity.id,
      description = entity.description,
      flagNonTranslatable = entity.flagNonTranslatable,
      flagCaseSensitive = entity.flagCaseSensitive,
      flagAbbreviation = entity.flagAbbreviation,
      flagForbiddenTerm = entity.flagForbiddenTerm,
      translations = entity.translations?.map { glossaryTermTranslationModelAssembler.toModel(it) } ?: emptyList(),
    )
  }
}
