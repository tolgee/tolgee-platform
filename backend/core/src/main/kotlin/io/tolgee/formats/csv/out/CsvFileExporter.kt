package io.tolgee.formats.csv.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.out.TableExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import java.io.InputStream

class CsvFileExporter(
  translations: List<ExportTranslationView>,
  exportParams: IExportParams,
  isProjectIcuPlaceholdersEnabled: Boolean = true,
  filePathProvider: ExportFilePathProvider,
) : TableExporter(translations, exportParams, isProjectIcuPlaceholdersEnabled, filePathProvider) {
  override fun List<TableEntry>.toFileContents(): InputStream {
    val languageTags =
      exportParams.languages?.sorted()?.toTypedArray()
        ?: this
          .map { it.language }
          .distinct()
          .sorted()
          .toTypedArray()
    return CsvFileWriter(
      languageTags = languageTags,
      data = this,
      delimiter = ',',
    ).produceFiles()
  }
}
