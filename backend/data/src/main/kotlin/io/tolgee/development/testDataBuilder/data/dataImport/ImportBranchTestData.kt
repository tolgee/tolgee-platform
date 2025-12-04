package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.branching.Branch

class ImportBranchTestData : BaseTestData() {
  var featureBranch: Branch

  init {
    this.root.apply {
      projectBuilder.apply {
        featureBranch =
          addBranch {
            name = "feature"
            project = projectBuilder.self
            isProtected = false
            isDefault = false
            originBranch = this
          }.self
      }
    }
  }
}
