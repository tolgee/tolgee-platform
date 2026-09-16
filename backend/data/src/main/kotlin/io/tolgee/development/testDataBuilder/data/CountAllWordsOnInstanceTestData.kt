package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.branching.Branch

class CountAllWordsOnInstanceTestData {
  val root = TestDataBuilder()

  lateinit var firstOrg: Organization
  lateinit var secondOrg: Organization

  init {
    root.apply {
      addFirstOrgScenario()
      addSecondOrgScenario()
    }
  }

  private fun TestDataBuilder.addFirstOrgScenario() {
    val userBuilder =
      addUserAccount {
        username = "caw-first-org-user"
      }

    firstOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "First CAW Project"
      organizationOwner = firstOrg
      useBranching = false
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      addKey { name = "caw-key1" }.build {
        addTranslation("en", "hello world")
      }
    }
  }

  private fun TestDataBuilder.addSecondOrgScenario() {
    val userBuilder =
      addUserAccount {
        username = "caw-second-org-user"
      }

    secondOrg = userBuilder.defaultOrganizationBuilder.self

    lateinit var mainBranch: Branch
    lateinit var featureBranch: Branch

    addProject {
      name = "Second CAW Project"
      organizationOwner = secondOrg
      useBranching = true
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      addBranch {
        name = "main"
        project = self
        isDefault = true
      }.build { mainBranch = self }

      addBranch {
        name = "feature"
        project = self
        originBranch = mainBranch
      }.build { featureBranch = self }

      addKey {
        name = "caw-key2"
        branch = mainBranch
      }.build {
        addTranslation("en", "one")
      }
      addKey {
        name = "caw-key2"
        branch = featureBranch
      }.build {
        addTranslation("en", "hello world there")
      }
    }
  }
}
