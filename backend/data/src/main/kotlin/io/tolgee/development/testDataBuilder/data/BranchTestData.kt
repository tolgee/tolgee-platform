package io.tolgee.development.testDataBuilder.data

import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.branching.Branch
import io.tolgee.util.addDays

class BranchTestData(
  private var currentDateProvider: CurrentDateProvider
) : BaseTestData("branch", "Project with branches") {
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  init {
    projectBuilder.apply {
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
  }
}
