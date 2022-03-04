package io.tolgee.service.export.exporters

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.service.export.dataProvider.ExportTranslationView
import java.io.InputStream

interface FileExporter {
  val translations: List<ExportTranslationView>
  val exportParams: ExportParams
  val fileExtension: String

  fun produceFiles(): Map<String, InputStream>

  fun getRealScopeDepth(path: List<String>): Int {
    return if (exportParams.splitByScope && exportParams.splitByScopeDepth > 0 && path.size > 1)
      exportParams.splitByScopeDepth
    else 0
  }

  fun ExportTranslationView.getFileAbsolutePath(path: List<String>): String {
    val filename = "${this.languageTag}.$fileExtension"
    val filePath = path.take(getRealScopeDepth(path)).joinToString("/")
    return "$filePath/$filename"
  }
}
