package io.tolgee.formats.json.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredRawDataToTextConvertor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class JsonFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    val data =
      try {
        objectMapper.readValue<Any?>(context.file.data)
      } catch (e: Exception) {
        throw ImportCannotParseFileException(context.file.name, e.message, e)
      }
    val format = getFormat(data)
    GenericStructuredProcessor(
      context = context,
      data = data,
      format = format,
      convertor =
        GenericStructuredRawDataToTextConvertor(
          format = format,
          firstLanguageTagGuessOrUnknown,
        ),
    ).process()
  }

  private fun getFormat(data: Any?): ImportFormat {
    return context.mapping?.format ?: JsonImportFormatDetector().detectFormat(data)
  }
}
