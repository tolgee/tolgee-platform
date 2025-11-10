package io.tolgee.formats.xlsx.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.out.TableExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import java.io.InputStream
import java.util.Date

class XlsxFileExporter(
  val currentDate: Date,
  translations: List<ExportTranslationView>,
  exportParams: IExportParams,
  isProjectIcuPlaceholdersEnabled: Boolean = true,
  pathProvider: ExportFilePathProvider,
) : TableExporter(
    translations,
    exportParams,
    isProjectIcuPlaceholdersEnabled,
    pathProvider,
  ) {
  override fun List<TableEntry>.toFileContents(): InputStream {
    val languageTags =
      exportParams.languages?.sorted()?.toTypedArray()
        ?: this
          .map { it.language }
          .distinct()
          .sorted()
          .toTypedArray()
    return XlsxFileWriter(
      createdDate = currentDate,
      languageTags = languageTags,
      data = this,
    ).produceFiles()
  }
}
