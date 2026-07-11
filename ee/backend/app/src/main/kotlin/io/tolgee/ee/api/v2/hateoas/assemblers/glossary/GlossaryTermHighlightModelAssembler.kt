package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryTermHighlightsController
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermHighlightModel
import io.tolgee.ee.data.glossary.GlossaryTermHighlight
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class GlossaryTermHighlightModelAssembler(
  private val glossaryTermModelAssembler: GlossaryTermModelAssembler,
  private val positionModelAssembler: PositionModelAssembler,
) : RepresentationModelAssemblerSupport<GlossaryTermHighlight, GlossaryTermHighlightModel>(
    GlossaryTermHighlightsController::class.java,
    GlossaryTermHighlightModel::class.java,
  ) {
  override fun toModel(entity: GlossaryTermHighlight): GlossaryTermHighlightModel {
    return GlossaryTermHighlightModel(
      position = positionModelAssembler.toModel(entity.position),
      value = glossaryTermModelAssembler.toModel(entity.value.term),
    )
  }
}
