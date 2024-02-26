package io.tolgee.formats

import StringsdictFileProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.android.`in`.AndroidStringsXmlProcessor
import io.tolgee.formats.apple.`in`.strings.StringsFileProcessor
import io.tolgee.formats.flutter.`in`.FlutterArbFileProcessor
import io.tolgee.formats.json.`in`.NativeJsonFileProcessor
import io.tolgee.formats.po.`in`.PoFileProcessor
import io.tolgee.formats.xliff.`in`.XliffFileProcessor
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportArchiveProcessor
import io.tolgee.service.dataImport.processors.PropertyFileProcessor
import io.tolgee.service.dataImport.processors.ZipTypeProcessor
import org.springframework.stereotype.Component

@Component
class ImportFileProcessorFactory(
  private val objectMapper: ObjectMapper,
) {
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
      "json" -> NativeJsonFileProcessor(context)
      "po" -> PoFileProcessor(context)
      "strings" -> StringsFileProcessor(context)
      "stringsdict" -> StringsdictFileProcessor(context)
      "xliff" -> XliffFileProcessor(context)
      "xlf" -> XliffFileProcessor(context)
      "properties" -> PropertyFileProcessor(context)
      "xml" -> AndroidStringsXmlProcessor(context)
      "arb" -> FlutterArbFileProcessor(context, objectMapper)
      else -> throw ImportCannotParseFileException(file.name, "No matching processor")
    }
  }

  val String?.fileNameExtension: String?
    get() {
      return this?.replace(".*\\.(.+)\$".toRegex(), "$1")
    }
}
