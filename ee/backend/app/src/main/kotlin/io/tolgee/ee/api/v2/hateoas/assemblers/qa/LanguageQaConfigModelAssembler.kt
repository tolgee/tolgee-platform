package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.ee.api.v2.hateoas.model.qa.LanguageQaConfigModel
import io.tolgee.hateoas.language.LanguageModelAssembler
import io.tolgee.model.qa.LanguageQaConfig
import org.springframework.stereotype.Component

@Component
class LanguageQaConfigModelAssembler(
  private val languageModelAssembler: LanguageModelAssembler,
) {
  fun toModel(
    entity: LanguageQaConfig?,
    lang: LanguageDto,
  ): LanguageQaConfigModel {
    return LanguageQaConfigModel(
      language = languageModelAssembler.toModel(lang),
      customSettings = entity?.settings?.toMap(),
    )
  }
}
