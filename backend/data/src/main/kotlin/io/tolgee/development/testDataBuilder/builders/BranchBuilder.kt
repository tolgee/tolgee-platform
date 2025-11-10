package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.Branch

class BranchBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Branch, BranchBuilder>() {
  override val self: Branch =
    Branch().apply {
      this.project = projectBuilder.self
    }.run {
      projectBuilder.self.branches.add(this)
      this
    }
}
