package io.tolgee.service.export.exporters

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.model.translation.Translation
import java.io.InputStream

interface FileExporter {
  val translations: List<Translation>
  val exportParams: ExportParams
  val fileExtension: String

  fun produceFiles(): Map<String, InputStream>

  fun getRealScopeDepth(path: List<String>): Int {
    return if (exportParams.splitByScope && exportParams.splitByScopeDepth > 0 && path.size > 1)
      exportParams.splitByScopeDepth
    else 0
  }

  fun Translation.getFileAbsolutePath(path: List<String>): String {
    val filename = "${this.language.tag}.$fileExtension"
    val filePath = path.take(getRealScopeDepth(path)).joinToString("/")
    return "$filePath/$filename"
  }
}
