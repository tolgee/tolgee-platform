package io.tolgee.service.import.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportFileProcessorResult
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessage
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.*
import org.springframework.stereotype.Component
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Component
class ZipTypeProcessor : ImportFileTypeProcessor {
    override fun process(
            file: ImportFileDto,
            messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit
    ): ImportFileProcessorResult {
        val zipInputStream = ZipInputStream(file.inputStream)
        var nextEntry: ZipEntry?
        val files = mutableListOf<ImportFileDto>()
        while (zipInputStream.nextEntry.also { nextEntry = it } != null) {
            files.add(ImportFileDto(name = nextEntry!!.name, zipInputStream.buffered()))
        }
        zipInputStream.close()
        return ImportFileProcessorResult(files)
    }
}
