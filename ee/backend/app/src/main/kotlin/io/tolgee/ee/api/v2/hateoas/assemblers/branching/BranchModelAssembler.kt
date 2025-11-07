package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.branching.Branch
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchModelAssembler(
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler
) : RepresentationModelAssembler<Branch, BranchModel> {
  override fun toModel(entity: Branch): BranchModel {
    return BranchModel(
      id = entity.id,
      name = entity.name,
      author = entity.author?.let { simpleUserAccountModelAssembler.toModel(it) },
      active = entity.archivedAt == null,
      isDefault = entity.isDefault,
      isProtected = entity.isProtected,
      createdAt = entity.createdAt?.time,
    )
  }
}
