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
    val shouldSplitToFiles = exportParams.splitByScope && exportParams.splitByScopeDepth > 0
    val isPathLongEnough = path.size > exportParams.splitByScopeDepth

    if (shouldSplitToFiles) {
      if (isPathLongEnough) {
        return exportParams.splitByScopeDepth
      }

      // we always need to keep some keyName for the file
      if (path.size > 1) {
        return path.size - 1
      }
    }

    return 0
  }

  fun ExportTranslationView.getFileAbsolutePath(path: List<String>): String {
    val filename = "${this.languageTag}.$fileExtension"
    val filePath = path.take(getRealScopeDepth(path)).joinToString("/")
    return "$filePath/$filename"
  }
}
