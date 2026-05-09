package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.Language
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.key.Key
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.translation.Translation

/**
 * Test data for QA language-stats refresh scenarios that involve branches.
 *
 * Contains a project with branching enabled, two branches (`main` + `feature`), and
 * one `fr` translation on each branch so stats can be asserted independently per
 * branch.
 */
class QaLanguageStatsBranchTestData : BaseTestData() {
  lateinit var frenchLanguage: Language

  lateinit var mainBranch: Branch
  lateinit var featureBranch: Branch

  lateinit var mainKey: Key
  lateinit var mainFrTranslation: Translation

  lateinit var featureKey: Key
  lateinit var featureFrTranslation: Translation

  init {
    project.useQaChecks = true
    projectBuilder.apply {
      self.useBranching = true
      frenchLanguage = addFrench().self

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

      addKey {
        name = "main-key"
        branch = mainBranch
      }.build {
        addTranslation("en", "Hello world.")
        mainFrTranslation = addTranslation("fr", "bonjour monde").self
      }.also { mainKey = it.self }

      addKey {
        name = "feature-key"
        branch = featureBranch
      }.build {
        addTranslation("en", "Feature hello.")
        featureFrTranslation = addTranslation("fr", "bonjour feature").self
      }.also { featureKey = it.self }
    }
  }

  /**
   * Creates QA config with all check types enabled at WARNING, except SPELLING and GRAMMAR
   * which are OFF (they depend on an external LanguageTool container).
   */
  fun createDefaultQaConfig(): ProjectQaConfig {
    return ProjectQaConfig(
      project = project,
      settings =
        QaCheckType.entries
          .associateWith { type ->
            when (type) {
              QaCheckType.SPELLING, QaCheckType.GRAMMAR -> QaCheckSeverity.OFF
              else -> QaCheckSeverity.WARNING
            }
          }.toMutableMap(),
    )
  }
}
