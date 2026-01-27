package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch

class OrganizationStatsTestData : BaseTestData("org-stats", "Stats Project") {
  lateinit var organization: Organization
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch
  lateinit var secondProject: Project
  lateinit var secondProjectMainBranch: Branch
  lateinit var secondProjectFeatureBranch: Branch
  lateinit var germanLanguage: Language
  lateinit var secondProjectGermanLanguage: Language

  init {
    root.apply {
      organization = projectBuilder.self.organizationOwner

      projectBuilder.apply {
        self.useBranching = true
        addGermanLanguage()
        addBranchesAndKeys()
      }

      addSecondProject()
    }
  }

  private fun ProjectBuilder.addGermanLanguage() {
    addLanguage {
      name = "German"
      tag = "de"
      originalName = "Deutsch"
      germanLanguage = this
    }
  }

  private fun ProjectBuilder.addBranchesAndKeys() {
    addBranch {
      name = "main"
      project = projectBuilder.self
      isDefault = true
    }.build {
      mainBranch = self
    }

    addBranch {
      name = "feature"
      project = projectBuilder.self
      originBranch = mainBranch
    }.build {
      featureBranch = self
    }

    // Key "key1" exists only in main branch - has EN translation
    addKey {
      name = "key1"
      branch = mainBranch
    }.build {
      addTranslation("en", "Key 1 English")
    }

    // Key "key2" exists in both branches (should be counted once) - has EN and DE translations in both branches
    addKey {
      name = "key2"
      branch = mainBranch
    }.build {
      addTranslation("en", "Key 2 English Main")
      addTranslation("de", "Key 2 German Main")
    }
    addKey {
      name = "key2"
      branch = featureBranch
    }.build {
      addTranslation("en", "Key 2 English Feature")
      addTranslation("de", "Key 2 German Feature")
    }

    // Key "key3" exists only in feature branch - has EN translation
    addKey {
      name = "key3"
      branch = featureBranch
    }.build {
      addTranslation("en", "Key 3 English")
    }

    // Key "key4" with namespace in main branch - has DE translation
    val namespace1 = addNamespace {
      name = "namespace1"
    }.self

    addKey {
      name = "key4"
      branch = mainBranch
      namespace = namespace1
    }.build {
      addTranslation("de", "Key 4 NS1 German Main")
    }

    // Key "key4" with same namespace in feature branch (should be counted once) - has DE translation
    addKey {
      name = "key4"
      branch = featureBranch
      namespace = namespace1
    }.build {
      addTranslation("de", "Key 4 NS1 German Feature")
    }

    // Key "key4" with different namespace (should be counted as separate) - no translations
    val namespace2 = addNamespace {
      name = "namespace2"
    }.self

    addKey {
      name = "key4"
      branch = mainBranch
      namespace = namespace2
    }

    // Key "key4" without namespace (should be counted as separate from namespaced ones) - has empty translation (should not count)
    addKey {
      name = "key4"
      branch = mainBranch
    }.build {
      addTranslation("en", "")
    }
  }

  private fun TestDataBuilder.addSecondProject() {
    secondProject = addProject {
      name = "Second Project"
      organizationOwner = this@OrganizationStatsTestData.organization
      useBranching = true
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      addLanguage {
        name = "German"
        tag = "de"
        originalName = "Deutsch"
        secondProjectGermanLanguage = this
      }

      addBranch {
        name = "main"
        project = self
        isDefault = true
      }.build {
        secondProjectMainBranch = self
      }

      addBranch {
        name = "feature"
        project = self
        originBranch = secondProjectMainBranch
      }.build {
        secondProjectFeatureBranch = self
      }

      // Key "key1" in second project (same name as first project, should be counted separately) - has EN translation
      addKey {
        name = "key1"
        branch = secondProjectMainBranch
      }.build {
        addTranslation("en", "Second Project Key 1 English Main")
      }

      // Key "key1" in second project feature branch (should NOT be counted separately) - has EN translation
      addKey {
        name = "key1"
        branch = secondProjectFeatureBranch
      }.build {
        addTranslation("en", "Second Project Key 1 English Feature")
      }

      // Key "key5" only in second project - has EN and DE translations
      addKey {
        name = "key5"
        branch = secondProjectMainBranch
      }.build {
        addTranslation("en", "Key 5 English")
        addTranslation("de", "Key 5 German")
      }
    }.self
  }
}
