package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Language
import io.tolgee.model.qa.LanguageQaConfig
import java.util.Date

class LanguageBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Language, LanguageBuilder> {
  class DATA {
    var qaConfig: LanguageQaConfigBuilder? = null
  }

  val data = DATA()

  override var self: Language =
    Language().apply {
      project = projectBuilder.self
    }

  fun setQaConfig(ft: FT<LanguageQaConfig> = {}): LanguageQaConfigBuilder {
    val builder = LanguageQaConfigBuilder(this).apply { ft(self) }
    data.qaConfig = builder
    return builder
  }

  fun setDeletedAt(deletedAt: Date = Date()) {
    projectBuilder.testDataBuilder.rawUpdateAfterSave("update language set deleted_at = :deletedAt where id = :id") {
      mapOf("deletedAt" to deletedAt, "id" to self.id)
    }
  }
}
