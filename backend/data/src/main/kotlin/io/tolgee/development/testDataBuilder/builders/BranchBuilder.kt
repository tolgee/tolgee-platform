package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.Branch

class BranchBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Branch, BranchBuilder>() {
  override var self: Branch =
    Branch()
      .apply {
        this.project = projectBuilder.self
      }.also {
        projectBuilder.self.branches.add(it)
      }

  companion object {
    /**
     * Creates a BranchBuilder wrapping an existing branch (e.g., from Branch.createMainBranch).
     * Use this when you already have a Branch instance that was created elsewhere.
     */
    fun forExistingBranch(
      projectBuilder: ProjectBuilder,
      branch: Branch,
    ): BranchBuilder {
      return BranchBuilder(projectBuilder).also { builder ->
        // Remove the auto-created branch from project.branches before replacing with the existing one
        projectBuilder.self.branches.remove(builder.self)
        builder.self = branch
      }
    }
  }
}
