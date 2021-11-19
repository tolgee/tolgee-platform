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
      nextEntry?.name?.let { name ->
        if (!IGNORE_PREFIXES.any { name.startsWith(it) }) {
          files.add(ImportFileDto(name = nextEntry!!.name, zipInputStream.readAllBytes().inputStream()))
        }
      }
    }
    return files
  }
}
