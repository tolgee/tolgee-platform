package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.branching.Branch
import io.tolgee.model.key.Key

class SoftDeleteBranchingTestData : BaseTestData("soft_delete_branching", "Project for soft-delete branching tests") {
  lateinit var mainBranch: Branch
  lateinit var key1: Key
  lateinit var key2: Key
  lateinit var key3: Key

  init {
    projectBuilder.apply {
      self.useBranching = true

      mainBranch =
        addBranch {
          name = "main"
          isDefault = true
          isProtected = false
        }.self

      key1 =
        addKey {
          name = "key1"
          branch = mainBranch
        }.build {
          addTranslation {
            language = englishLanguage
            text = "Key 1 translation"
          }
        }.self

      key2 =
        addKey {
          name = "key2"
          branch = mainBranch
        }.build {
          addTranslation {
            language = englishLanguage
            text = "Key 2 translation"
          }
        }.self

      key3 =
        addKey {
          name = "key3"
          branch = mainBranch
        }.build {
          addTranslation {
            language = englishLanguage
            text = "Key 3 translation"
          }
        }.self
    }
  }
}
