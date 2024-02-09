package io.tolgee.service.dataImport.processors

import StringsdictFileProcessor
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.apple.`in`.strings.StringsFileProcessor
import io.tolgee.formats.json.`in`.JsonFileProcessor
import io.tolgee.formats.po.`in`.PoFileProcessor
import io.tolgee.formats.xliff.`in`.XliffFileProcessor
import org.springframework.stereotype.Component

@Component
class ProcessorFactory {
  fun getArchiveProcessor(file: ImportFileDto): ImportArchiveProcessor {
    return when (file.name.fileNameExtension) {
      "zip" -> ZipTypeProcessor()
      else -> throw ImportCannotParseFileException(file.name, "No matching processor")
    }
  }

  fun getProcessor(
    file: ImportFileDto,
    context: FileProcessorContext,
  ): ImportFileProcessor {
    return when (file.name.fileNameExtension) {
      "json" -> JsonFileProcessor(context)
      "po" -> PoFileProcessor(context)
      "strings" -> StringsFileProcessor(context)
      "stringsdict" -> StringsdictFileProcessor(context)
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
