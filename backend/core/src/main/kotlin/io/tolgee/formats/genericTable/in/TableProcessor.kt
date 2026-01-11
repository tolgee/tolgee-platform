package io.tolgee.formats.genericTable.`in`

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

abstract class TableProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    val (data, format) = parse()
    data.importAll(format)
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

  protected abstract fun parse(): Pair<Iterable<TableEntry>, ImportFormat>
}
