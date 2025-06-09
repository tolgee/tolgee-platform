package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.metadata.TranslationGlossaryItem
import io.tolgee.dtos.cacheable.ProjectDto
import org.springframework.stereotype.Service

@Service
class MtGlossaryTermsProviderOssImpl : MtGlossaryTermsProvider {
  override fun getGlossaryTerms(
    project: ProjectDto,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    text: String,
  ): Set<TranslationGlossaryItem> {
    return emptySet()
  }
}
