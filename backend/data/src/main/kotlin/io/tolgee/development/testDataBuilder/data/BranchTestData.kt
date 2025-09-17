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
          createdAt = currentDateProvider.date.addDays(1)
        }.build {
          featureBranch = self
        }
        addBranch {
          name = "merged-branch"
          project = projectBuilder.self
          isProtected = false
          isDefault = false
          createdAt = currentDateProvider.date.addDays(-2)
          archivedAt = currentDateProvider.date.addDays(-1)
          originBranch = this
        }
        addBranch {
          name = "merged-branch-older"
          project = projectBuilder.self
          isProtected = false
          createdAt = currentDateProvider.date.addDays(-3)
          archivedAt = currentDateProvider.date.addDays(-2)
        }
      }
    }
  }
}
