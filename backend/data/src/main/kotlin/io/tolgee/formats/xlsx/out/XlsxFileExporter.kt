package io.tolgee.formats.xlsx.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.out.TableExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import java.io.InputStream

class XlsxFileExporter(
  translations: List<ExportTranslationView>,
  exportParams: IExportParams,
  isProjectIcuPlaceholdersEnabled: Boolean = true,
) : TableExporter(translations, exportParams, "xlsx", isProjectIcuPlaceholdersEnabled) {
  override fun List<TableEntry>.toFileContents(): InputStream {
    val languageTags =
      exportParams.languages?.sorted()?.toTypedArray()
        ?: this.map { it.language }.distinct().sorted().toTypedArray()
    return XlsxFileWriter(
      languageTags = languageTags,
      data = this,
    ).produceFiles()
  }
}
