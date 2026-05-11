package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.qa.LanguageQaConfig

class LanguageQaConfigBuilder(
  val languageBuilder: LanguageBuilder,
) : EntityDataBuilder<LanguageQaConfig, LanguageQaConfigBuilder> {
  override var self: LanguageQaConfig = LanguageQaConfig(language = languageBuilder.self)
}
