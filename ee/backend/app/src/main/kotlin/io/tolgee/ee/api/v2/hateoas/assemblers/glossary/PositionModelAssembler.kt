package io.tolgee.ee.api.v2.hateoas.assemblers.glossary

import io.tolgee.ee.api.v2.controllers.glossary.GlossaryTermHighlightsController
import io.tolgee.ee.api.v2.hateoas.model.glossary.PositionModel
import io.tolgee.ee.data.glossary.Position
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class PositionModelAssembler :
  RepresentationModelAssemblerSupport<Position, PositionModel>(
    GlossaryTermHighlightsController::class.java,
    PositionModel::class.java,
  ) {
  override fun toModel(entity: Position): PositionModel {
    return PositionModel(
      start = entity.start,
      end = entity.end,
    )
  }
}
