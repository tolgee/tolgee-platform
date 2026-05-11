package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.qa.LanguageQaConfig

class LanguageQaConfigBuilder(
  val projectBuilder: ProjectBuilder,
  language: Language,
) : EntityDataBuilder<LanguageQaConfig, LanguageQaConfigBuilder> {
  override var self: LanguageQaConfig = LanguageQaConfig(language = language)
}
