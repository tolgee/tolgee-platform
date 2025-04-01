package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryTermController
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermTranslationModel
import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class GlossaryTermTranslationModelAssembler :
  RepresentationModelAssemblerSupport<GlossaryTermTranslation, GlossaryTermTranslationModel>(
    GlossaryTermController::class.java,
    GlossaryTermTranslationModel::class.java,
  ) {
  override fun toModel(entity: GlossaryTermTranslation): GlossaryTermTranslationModel {
    return GlossaryTermTranslationModel(
      id = entity.id,
      languageCode = entity.languageCode,
      text = entity.text,
    )
  }
}
