package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel
import io.tolgee.model.branching.Branch
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchModelAssembler : RepresentationModelAssembler<Branch, BranchModel> {
  override fun toModel(entity: Branch): BranchModel {
    return BranchModel(
        id = entity.id,
        name = entity.name,
        active = entity.archivedAt == null,
        isDefault = entity.isDefault,
        isProtected = entity.isProtected,
    )
  }
}
