package io.tolgee.service.export.exporters

import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.model.ILanguage
import io.tolgee.service.export.dataProvider.ExportTranslationView
import java.io.InputStream

class PoFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  baseTranslationsProvider: () -> List<ExportTranslationView>,
  val baseLanguage: ILanguage,
) : FileExporter {
  override val fileExtension: String = ExportFormat.PO.extension

  override fun produceFiles(): Map<String, InputStream> {
    TODO()
  }
}
