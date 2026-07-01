package io.tolgee.formats.genericTable.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

abstract class TableProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    val (data, format) = parse()
    data.validateStructure()
    data.importAll(format)
  }

  private fun List<TableEntry>.validateStructure() {
    if (none()) return
    if (none { it.key.isNotBlank() }) {
      throw ImportCannotParseFileException(
        context.file.name,
        "The key column (first column) is empty in every row. " +
          "The first column must contain the key names.",
      )
    }
    if (none { it.language.isNotBlank() }) {
      throw ImportCannotParseFileException(
        context.file.name,
        "No language columns were detected. The first row must be a header with the key column " +
          "followed by a language tag (e.g. \"en\") for each translation column. " +
          "Make sure there is no extra title row above the header.",
      )
    }
  }

  fun Iterable<TableEntry>.importAll(format: ImportFormat) {
    forEachIndexed { idx, it -> it.import(idx, format) }
  }

  fun TableEntry.import(
    index: Int,
    format: ImportFormat,
  ) {
    val converted =
      format.messageConvertor.convert(
        value,
        language,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )
    context.addTranslation(
      key,
      language,
      converted.message,
      index,
      pluralArgName = converted.pluralArgName,
      rawData = value,
      convertedBy = format,
    )
  }

  protected abstract fun parse(): Pair<List<TableEntry>, ImportFormat>
}
