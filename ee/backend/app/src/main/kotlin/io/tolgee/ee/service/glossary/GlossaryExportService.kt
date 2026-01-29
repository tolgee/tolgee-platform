package io.tolgee.ee.service.glossary

import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.ee.service.glossary.formats.csv.out.GlossaryCSVExporter
import io.tolgee.model.glossary.Glossary
import io.tolgee.service.language.LanguageService
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Duration

@Service
class GlossaryExportService(
  private val glossaryService: GlossaryService,
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
  private val languageService: LanguageService,
  private val businessEventPublisher: BusinessEventPublisher,
) {
  fun exportCsv(
    glossary: Glossary,
    delimiter: Char = ',',
  ): InputStream {
    val terms = glossaryTermService.findAllWithTranslations(glossary)
    val glossaryLanguageTags = glossaryTermTranslationService.getDistinctLanguageTags(glossary)
    val organizationLanguageTags = getOrganizationLanguageTagsForExport(glossary)
    return GlossaryCSVExporter(
      glossary,
      terms,
      glossaryLanguageTags + organizationLanguageTags,
      delimiter,
    ).export().also {
      publishBusinessEvent(glossary.id)
    }
  }

  private fun getOrganizationLanguageTagsForExport(glossary: Glossary): Set<String> {
    val assignedProjectIds = glossaryService.getAssignedProjectsIds(glossary)
    if (assignedProjectIds.isEmpty()) {
      return languageService.getTagsByOrganization(glossary.organizationOwner.id)
    }

    return languageService.getTagsByOrganizationAndProjectIds(
      glossary.organizationOwner.id,
      assignedProjectIds.toList(),
    )
  }

  private fun publishBusinessEvent(glossaryId: Long) {
    businessEventPublisher.publishOnceInTime(
      OnBusinessEventToCaptureEvent(
        eventName = "GLOSSARY_EXPORT",
        glossaryId = glossaryId,
      ),
      Duration.ofDays(1),
    ) {
      "GLOSSARY_EXPORT_$glossaryId"
    }
  }
}
