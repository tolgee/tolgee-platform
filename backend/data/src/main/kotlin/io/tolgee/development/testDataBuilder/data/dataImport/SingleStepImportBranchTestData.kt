package io.tolgee.development.testDataBuilder.data.dataImport

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.branching.Branch

class SingleStepImportBranchTestData : BaseTestData() {
  val germanLanguage = projectBuilder.addGerman()
  var defaultBranch: Branch
  var featureBranch: Branch

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
        addKey {
          name = "shared"
          branch = defaultBranch
        }.build { setDescription("Main description") }
        (1..3).forEach { num ->
          addKeyWithCodeReference("meta key $num", defaultBranch, if (num < 3) "ns$num" else null)
        }
        addKeyWithCodeReference("feature meta key", featureBranch, null)
      }
    }
  }

  private fun ProjectBuilder.addKeyWithCodeReference(
    keyName: String,
    keyBranch: Branch,
    namespace: String?,
  ) {
    addKey {
      name = keyName
      branch = keyBranch
    }.build {
      setNamespace(namespace)
      addMeta {
        addCodeReference(user) { path = "src/$keyName.swift" }
      }
    }
  }
}
