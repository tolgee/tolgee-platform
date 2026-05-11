package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig

/**
 * Test data covering every QA-related persistent entity on a project so the
 * hard-delete flow can be exercised against all of them at once: a translation
 * with a [io.tolgee.model.qa.TranslationQaIssue] (added through the builder)
 * plus a [ProjectQaConfig] and a [LanguageQaConfig] which are exposed via
 * factory methods because the builder framework does not yet model QA configs.
 *
 * Lives in the data module — and not in the EE-only `QaTestData` — so tests in
 * `:server-app` (which is built without `:ee-app` for the `runWithoutEeTests`
 * task) can still use it.
 */
class ProjectWithQaEntitiesTestData : BaseTestData() {
  init {
    projectBuilder.addKey(keyName = "test-key") {
      addTranslation("en", "Hello world.").build {
        addQaIssue {
          type = QaCheckType.EMPTY_TRANSLATION
          message = QaIssueMessage.QA_EMPTY_TRANSLATION
        }
      }
    }
  }

  /**
   * Builds (but does NOT persist) a [ProjectQaConfig] for [project]. The caller
   * persists it through the repository so we don't have to model QA configs in
   * the builder framework.
   */
  fun createProjectQaConfig(): ProjectQaConfig = ProjectQaConfig(project = project)

  /**
   * Builds (but does NOT persist) a [LanguageQaConfig] for [englishLanguage].
   * See [createProjectQaConfig] for why this is a factory rather than seeded
   * by the builder.
   */
  fun createLanguageQaConfig(): LanguageQaConfig = LanguageQaConfig(language = englishLanguage)
}
