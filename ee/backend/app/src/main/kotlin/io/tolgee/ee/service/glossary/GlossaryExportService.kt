package io.tolgee.ee.service.glossary

import io.tolgee.ee.service.glossary.formats.csv.out.GlossaryCSVExporter
import io.tolgee.model.glossary.Glossary
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class GlossaryExportService(
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
) {
  fun exportCsv(
    glossary: Glossary,
    delimiter: Char = ',',
  ): InputStream {
    val terms = glossaryTermService.findAllWithTranslations(glossary)
    val languageTags = glossaryTermTranslationService.getDistinctLanguageTags(glossary)
    return GlossaryCSVExporter(glossary, terms, languageTags, delimiter).export()
  }
}
