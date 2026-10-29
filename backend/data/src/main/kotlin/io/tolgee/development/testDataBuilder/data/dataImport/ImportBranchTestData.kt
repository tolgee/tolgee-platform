package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.branching.Branch

class ImportBranchTestData : BaseTestData() {
  var featureBranch: Branch
  var protectedBranch: Branch

  init {
    this.root.apply {
      projectBuilder.apply {
        self.useBranching = true
        featureBranch =
          addBranch {
            name = "feature"
            project = projectBuilder.self
            isProtected = false
            isDefault = false
            originBranch = this
          }.self
        protectedBranch =
          addBranch {
            name = "protected"
            project = projectBuilder.self
            isProtected = true
            isDefault = false
            originBranch = this
          }.self
      }
    }
  }
}
