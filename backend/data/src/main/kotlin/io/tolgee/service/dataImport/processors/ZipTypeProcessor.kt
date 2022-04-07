package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipTypeProcessor : ImportArchiveProcessor {
  companion object {
    val IGNORE_PREFIXES = arrayOf("__", ".")
  }

  override fun process(file: ImportFileDto): MutableList<ImportFileDto> {
    val zipInputStream = ZipInputStream(file.inputStream)
    var nextEntry: ZipEntry?
    val files = mutableListOf<ImportFileDto>()
    while (zipInputStream.nextEntry.also { nextEntry = it } != null) {
      val fileName = nextEntry?.name?.replaceRootSlash() ?: continue

      if (!IGNORE_PREFIXES.any { fileName.startsWith(it) }) {
        files.add(
          ImportFileDto(
            name = fileName,
            zipInputStream.readAllBytes().inputStream()
          )
        )
      }
    }
    return files
  }

  private fun String.replaceRootSlash() = this.replace("^/".toRegex(), "")
}
