package io.tolgee.ee.service.glossary

import io.tolgee.ee.service.glossary.formats.csv.out.GlossaryCSVExporter
import io.tolgee.model.glossary.Glossary
import io.tolgee.service.language.LanguageService
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class GlossaryExportService(
  private val glossaryService: GlossaryService,
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
  private val languageService: LanguageService,
) {
  fun exportCsv(
    glossary: Glossary,
    delimiter: Char = ',',
  ): InputStream {
    val terms = glossaryTermService.findAllWithTranslations(glossary)
    val glossaryLanguageTags = glossaryTermTranslationService.getDistinctLanguageTags(glossary)
    val organizationLanguageTags = getOrganizationLanguageTagsForExport(glossary)
    return GlossaryCSVExporter(glossary, terms, glossaryLanguageTags + organizationLanguageTags, delimiter).export()
  }

  private fun getOrganizationLanguageTagsForExport(glossary: Glossary): Set<String> {
    val assignedProjectIds = glossaryService.getAssignedProjectsIds(glossary)
    return languageService
      .getTagsByOrganization(
        organizationId = glossary.organizationOwner.id,
        projectIds = assignedProjectIds.ifEmpty { null }?.toList(),
      )
  }
}
