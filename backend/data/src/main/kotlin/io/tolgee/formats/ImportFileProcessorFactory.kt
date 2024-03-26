package io.tolgee.formats

import StringsdictFileProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.android.`in`.AndroidStringsXmlProcessor
import io.tolgee.formats.apple.`in`.strings.StringsFileProcessor
import io.tolgee.formats.flutter.`in`.FlutterArbFileProcessor
import io.tolgee.formats.json.`in`.JsonFileProcessor
import io.tolgee.formats.po.`in`.PoFileProcessor
import io.tolgee.formats.properties.`in`.PropertiesFileProcessor
import io.tolgee.formats.xliff.`in`.XliffFileProcessor
import io.tolgee.formats.yaml.`in`.YamlFileProcessor
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportArchiveProcessor
import io.tolgee.service.dataImport.processors.ZipTypeProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ImportFileProcessorFactory(
  private val objectMapper: ObjectMapper,
  @Qualifier("yamlObjectMapper")
  private val yamlObjectMapper: ObjectMapper,
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
      "json" -> JsonFileProcessor(context, objectMapper)
      "po" -> PoFileProcessor(context)
      "strings" -> StringsFileProcessor(context)
      "stringsdict" -> StringsdictFileProcessor(context)
      "xliff", "xlf" -> XliffFileProcessor(context)
      "properties" -> PropertiesFileProcessor(context)
      "xml" -> AndroidStringsXmlProcessor(context)
      "arb" -> FlutterArbFileProcessor(context, objectMapper)
      "yaml", "yml" -> YamlFileProcessor(context, yamlObjectMapper)
      else -> throw ImportCannotParseFileException(file.name, "No matching processor")
    }
  }

  val String?.fileNameExtension: String?
    get() {
      return this?.replace(".*\\.(.+)\$".toRegex(), "$1")
    }
}
