package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.Scope

class McpScreenshotBranchTestData : BaseTestData("mcp_branch_user", "mcp_branch_project") {
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  lateinit var pakWithProtectScope: ApiKey
  lateinit var pakWithoutProtectScope: ApiKey

  private val baseScopes =
    setOf(
      Scope.KEYS_CREATE,
      Scope.KEYS_EDIT,
      Scope.KEYS_VIEW,
      Scope.SCREENSHOTS_UPLOAD,
      Scope.SCREENSHOTS_VIEW,
      Scope.TRANSLATIONS_EDIT,
    )

  init {
    projectBuilder.apply {
      self.useBranching = true

      mainBranch =
        addBranch {
          name = "main"
          project = projectBuilder.self
          isProtected = true
          isDefault = true
        }.self

      featureBranch =
        addBranch {
          name = "feature"
          project = projectBuilder.self
          isProtected = false
          isDefault = false
          originBranch = mainBranch
        }.self

      addKey {
        name = "protected.existing"
        branch = mainBranch
      }
      addKey {
        name = "feature.existing"
        branch = featureBranch
      }

      addApiKey {
        key = "mcp_branch_without_protect_pak"
        scopesEnum = baseScopes.toMutableSet()
        userAccount = userAccountBuilder.self
        pakWithoutProtectScope = this
      }
      addApiKey {
        key = "mcp_branch_with_protect_pak"
        scopesEnum = (baseScopes + Scope.BRANCH_PROTECTED_MODIFY).toMutableSet()
        userAccount = userAccountBuilder.self
        pakWithProtectScope = this
      }
    }
  }
}
