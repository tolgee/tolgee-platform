package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.AutoTranslationConfig

class AutoTranslationConfigBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<AutoTranslationConfig, AutoTranslationConfigBuilder>() {
  override var self: AutoTranslationConfig = AutoTranslationConfig().apply { project = projectBuilder.self }
}
