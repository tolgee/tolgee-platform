package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.branching.Branch

class SingleStepImportBranchTestData : BaseTestData() {
  val germanLanguage = projectBuilder.addGerman()
  lateinit var defaultBranch: Branch
  lateinit var featureBranch: Branch

  init {
    this.root.apply {
      projectBuilder.apply {
        self.useBranching = true
        defaultBranch =
          addBranch {
            name = Branch.DEFAULT_BRANCH_NAME
            project = projectBuilder.self
            isDefault = true
            isProtected = true
          }.self
        featureBranch =
          addBranch {
            name = "feature"
            project = projectBuilder.self
            isDefault = false
            isProtected = false
            originBranch = defaultBranch
          }.self
      }
    }
  }
}
