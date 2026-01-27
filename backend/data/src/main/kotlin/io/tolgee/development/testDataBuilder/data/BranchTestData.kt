package io.tolgee.development.testDataBuilder.data

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.util.addDays

class BranchTestData(
  private var currentDateProvider: CurrentDateProvider,
) : BaseTestData("branch", "Project with branches") {
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  lateinit var mergeBranch: Branch
  lateinit var secondProject: Project
  lateinit var mergedBranchMerge: BranchMerge

  init {
    this.root.apply {
      projectBuilder.apply {
        self.useBranching = true
        // add test branches to test basic branch operations (create, update, delete)
        addBranches()
        // add keys to main and feature branches to test merging
        addMergeData()
      }
      // add a project without a branch to test default branch creation
      addProjectWithoutBranch()
    }
  }

  private fun TestDataBuilder.addProjectWithoutBranch() {
    secondProject =
      addProject {
        name = "empty-project"
        useBranching = true
      }.build {
        addKey {
          name = "test"
        }
      }.self
  }

  private fun ProjectBuilder.addBranches() {
    addBranch {
      name = "main"
      project = projectBuilder.self
      isProtected = true
      isDefault = true
      createdAt = currentDateProvider.date
      revision = 10
    }.build {
      mainBranch = self
      addBranch {
        name = "feature-branch"
        project = projectBuilder.self
        isProtected = false
        isDefault = false
        originBranch = self
        revision = 15
      }.build {
        featureBranch = self
        addBranch {
          name = "merge-branch"
          project = projectBuilder.self
          isProtected = false
          isDefault = false
          originBranch = self
          revision = 20
        }.build {
          mergeBranch = self
        }
      }
      addBranch {
        name = "merged-and-deleted-branch"
        project = projectBuilder.self
        isProtected = false
        isDefault = false
        deletedAt = currentDateProvider.date.addDays(-1)
        originBranch = this
      }
    }
  }

  private fun ProjectBuilder.addMergeData() {
    mergedBranchMerge =
      addBranchMerge {
        sourceBranch = mergeBranch
        targetBranch = mainBranch
        sourceRevision = 15
        targetRevision = 10
      }.self
  }
}
