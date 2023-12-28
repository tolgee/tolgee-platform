package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.service.dataImport.processors.po.PoFileProcessor
import io.tolgee.service.dataImport.processors.xliff.XliffFileProcessor
import org.springframework.stereotype.Component

@Component
class ProcessorFactory {
  fun getArchiveProcessor(file: ImportFileDto): ImportArchiveProcessor {
    return when (file.name.fileNameExtension) {
      "zip" -> ZipTypeProcessor()
      else -> throw ImportCannotParseFileException(file.name, "No matching processor")
    }
  }

  fun getProcessor(file: ImportFileDto, context: FileProcessorContext): ImportFileProcessor {
    return when (file.name.fileNameExtension) {
      "json" -> JsonFileProcessor(context)
      "po" -> PoFileProcessor(context)
      "xliff" -> XliffFileProcessor(context)
      "xlf" -> XliffFileProcessor(context)
      "properties" -> PropertyFileProcessor(context)
      else -> throw ImportCannotParseFileException(file.name, "No matching processor")
    }
  }

  val String?.fileNameExtension: String?
    get() {
      return this?.replace(".*\\.(.+)\$".toRegex(), "$1")
    }
}
