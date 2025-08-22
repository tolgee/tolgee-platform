package io.tolgee.formats

import StringsdictFileProcessor
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.apple.`in`.strings.StringsFileProcessor
import io.tolgee.formats.apple.`in`.xcstrings.XcstringsFileProcessor
import io.tolgee.formats.csv.`in`.CsvFileProcessor
import io.tolgee.formats.flutter.`in`.FlutterArbFileProcessor
import io.tolgee.formats.importCommon.ImportFileFormat
import io.tolgee.formats.json.`in`.JsonFileProcessor
import io.tolgee.formats.po.`in`.PoFileProcessor
import io.tolgee.formats.properties.`in`.PropertiesFileProcessor
import io.tolgee.formats.resx.`in`.ResxProcessor
import io.tolgee.formats.xliff.`in`.XliffFileProcessor
import io.tolgee.formats.xlsx.`in`.XlsxFileProcessor
import io.tolgee.formats.xmlResources.`in`.XmlResourcesProcessor
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
  private val tolgeeProperties: TolgeeProperties,
) {
  fun getArchiveProcessor(file: ImportFileDto): ImportArchiveProcessor {
    return when (file.name.fileNameExtension) {
      "zip" -> ZipTypeProcessor(tolgeeProperties.maxUploadFileSize.toLong() * 1024L)
      else -> throw ImportCannotParseFileException(file.name, "No matching processor")
    }
  }

  fun getProcessor(
    file: ImportFileDto,
    context: FileProcessorContext,
  ): ImportFileProcessor {
    val format =
      findFormatInMapping(context)
        ?: ImportFileFormat.findByExtension(file.name.fileNameExtension)
        ?: throw ImportCannotParseFileException(file.name, "No matching processor")

    return getProcessor(format, context)
  }

  private fun findFormatInMapping(context: FileProcessorContext) = context.mapping?.format?.fileFormat

  private fun getProcessor(
    importFileFormat: ImportFileFormat,
    context: FileProcessorContext,
  ): ImportFileProcessor {
    return when (importFileFormat) {
      ImportFileFormat.JSON -> JsonFileProcessor(context, objectMapper)
      ImportFileFormat.PO -> PoFileProcessor(context)
      ImportFileFormat.STRINGS -> StringsFileProcessor(context)
      ImportFileFormat.STRINGSDICT -> StringsdictFileProcessor(context)
      ImportFileFormat.XLIFF -> XliffFileProcessor(context)
      ImportFileFormat.PROPERTIES -> PropertiesFileProcessor(context)
      ImportFileFormat.XML -> XmlResourcesProcessor(context)
      ImportFileFormat.ARB -> FlutterArbFileProcessor(context, objectMapper)
      ImportFileFormat.YAML -> YamlFileProcessor(context, yamlObjectMapper)
      ImportFileFormat.CSV -> CsvFileProcessor(context)
      ImportFileFormat.RESX -> ResxProcessor(context)
      ImportFileFormat.XLSX -> XlsxFileProcessor(context)
      ImportFileFormat.XCSTRINGS -> XcstringsFileProcessor(context, objectMapper)
    }
  }

  val String?.fileNameExtension: String?
    get() {
      return this?.replace(".*\\.(.+)\$".toRegex(), "$1")
    }
}
