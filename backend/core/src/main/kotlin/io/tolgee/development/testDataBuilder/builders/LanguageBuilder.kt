package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Language

class LanguageBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Language, LanguageBuilder> {
  override var self: Language =
    Language().apply {
      project = projectBuilder.self
    }
}
