package io.tolgee.formats.json.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredProcessor
import io.tolgee.formats.genericStructuredFile.`in`.GenericStructuredRawDataToTextConvertor
import io.tolgee.formats.importMessageFormat.ImportMessageFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class JsonFileProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    val data = objectMapper.readValue<Any?>(context.file.data)
    GenericStructuredProcessor(
      context = context,
      data = data,
      convertor =
        GenericStructuredRawDataToTextConvertor(
          format = ImportMessageFormat.JSON,
          firstLanguageTagGuessOrUnknown,
        ),
    ).process()
  }
}
