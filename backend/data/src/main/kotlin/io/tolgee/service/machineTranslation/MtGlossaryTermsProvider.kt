package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.metadata.TranslationGlossaryItem
import io.tolgee.dtos.cacheable.ProjectDto

interface MtGlossaryTermsProvider {
  fun glossaryTermsFor(
    project: ProjectDto,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    text: String,
  ): Set<TranslationGlossaryItem>
}
