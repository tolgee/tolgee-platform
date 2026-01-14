package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.TranslationState

class ProjectStatsBranchingTestData :
  BaseTestData(
    "project_stats_branching_user",
    "Project stats branching",
  ) {
  var germanLanguage: Language
  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch

  init {
    projectBuilder.apply {
      germanLanguage =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }.self

      addBranches()
      addMainKeys()
      addFeatureKey()
    }
  }

  private fun ProjectBuilder.addBranches() {
    mainBranch =
      addBranch {
        name = "main"
        isDefault = true
        isProtected = true
      }.self

    featureBranch =
      addBranch {
        name = "feature"
        originBranch = mainBranch
      }.self
  }

  private fun ProjectBuilder.addMainKeys() {
    addKey {
      name = "main key 1"
      branch = mainBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = "one two"
        state = TranslationState.TRANSLATED
      }
      addTranslation {
        language = germanLanguage
        text = "eins zwei"
        state = TranslationState.TRANSLATED
      }
    }

    addKey {
      name = "main key 2"
      branch = mainBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = "three four"
        state = TranslationState.REVIEWED
      }
      addTranslation {
        language = germanLanguage
        text = "drei vier"
        state = TranslationState.REVIEWED
      }
    }
  }

  private fun ProjectBuilder.addFeatureKey() {
    addKey {
      name = "feature key"
      branch = featureBranch
    }.build {
      addTranslation {
        language = englishLanguage
        text = "alpha beta gamma"
        state = TranslationState.TRANSLATED
      }
      addTranslation {
        language = germanLanguage
        text = "eins"
        state = TranslationState.TRANSLATED
      }
    }
  }
}
