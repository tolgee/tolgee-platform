package io.tolgee.hateoas.quickStart

import io.tolgee.model.QuickStart
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class QuickStartModelAssembler() : RepresentationModelAssemblerSupport<QuickStart, QuickStartModel>(
  QuickStart::class.java, QuickStartModel::class.java
) {
  override fun toModel(entity: QuickStart): QuickStartModel {
    return QuickStartModel(
      finished = entity.finished,
      completedSteps = entity.completedSteps,
      open = entity.open
    )
  }
}
