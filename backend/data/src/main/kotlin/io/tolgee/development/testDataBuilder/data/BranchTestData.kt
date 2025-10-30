package io.tolgee.development.testDataBuilder.data

import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.util.addDays

class BranchTestData(
  private var currentDateProvider: CurrentDateProvider
) : BaseTestData("branch", "Project with branches") {
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  lateinit var secondProject: Project
  init {
    this.root.apply {
      projectBuilder.apply {
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
    secondProject = addProject {
      name = "empty-project"
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
    }.build {
      mainBranch = self
      addBranch {
        name = "feature-branch"
        project = projectBuilder.self
        isProtected = false
        isDefault = false
        originBranch = this
      }.build {
        featureBranch = self
      }
      addBranch {
        name = "merged-and-deleted-branch"
        project = projectBuilder.self
        isProtected = false
        isDefault = false
        archivedAt = currentDateProvider.date.addDays(-1)
        originBranch = this
      }
    }
  }

  private fun ProjectBuilder.addMergeData() {
    addKey {
      name = "test-key-to-delete"
      branch = mainBranch
    }.build {
      addTranslation("en", "main-key-target-translation-to-delete")
      addMeta {
        description = "Main key description to delete"
        addComment {
          text = "Main key comment to delete"
        }
      }
    }

    addKey {
      name = "key-to-add"
      branch = featureBranch
    }.build {
      addTranslation("en", "key-to-add-translation")
      addMeta {
        description = "Feature key description to add"
        addComment {
          text = "Feature key comment to add"
        }
      }
    }
  }
}
