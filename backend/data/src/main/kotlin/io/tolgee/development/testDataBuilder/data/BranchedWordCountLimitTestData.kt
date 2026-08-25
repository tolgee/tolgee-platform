package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.branching.Branch
import io.tolgee.model.translation.Translation

/**
 * The same key on two branches of a branching project, so the instance word count is the larger of
 * the two translations rather than their sum.
 */
class BranchedWordCountLimitTestData(
  defaultBranchWordCount: Int,
  featureBranchWordCount: Int,
) {
  val root = TestDataBuilder()

  lateinit var defaultBranchTranslation: Translation

  lateinit var featureBranchTranslation: Translation

  /** Unbranched and empty, so it contributes nothing until a test writes to it. */
  lateinit var unbranchedTranslation: Translation

  lateinit var project: Project

  lateinit var featureBranch: Branch

  init {
    root.apply {
      val userBuilder =
        addUserAccount {
          username = "branched-word-count-limit-user"
        }

      lateinit var mainBranch: Branch

      addProject {
        name = "Branched Word Count Limit Project"
        organizationOwner = userBuilder.defaultOrganizationBuilder.self
        useBranching = true
      }.build {
        project = self

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
          name = "bwcl-key1"
          branch = mainBranch
        }.build {
          addTranslation("en", WordCountLimitTestData.wordsText(defaultBranchWordCount))
            .build { defaultBranchTranslation = self }
        }

        addKey {
          name = "bwcl-key1"
          branch = featureBranch
        }.build {
          addTranslation("en", WordCountLimitTestData.wordsText(featureBranchWordCount))
            .build { featureBranchTranslation = self }
        }

        addKey {
          name = "bwcl-key2"
          branch = mainBranch
        }.build {
          addTranslation("en", "").build { unbranchedTranslation = self }
        }
      }
    }
  }
}
