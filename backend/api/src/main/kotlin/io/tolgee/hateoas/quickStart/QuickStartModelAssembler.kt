package io.tolgee.hateoas.quickStart

import io.tolgee.dtos.queryResults.organization.IQuickStart
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class QuickStartModelAssembler :
  RepresentationModelAssemblerSupport<IQuickStart, QuickStartModel>(
    IQuickStart::class.java,
    QuickStartModel::class.java,
  ) {
  override fun toModel(entity: IQuickStart): QuickStartModel {
    return QuickStartModel(
      finished = entity.finished,
      completedSteps = entity.completedSteps.toMutableList(),
      open = entity.open,
    )
  }
}
