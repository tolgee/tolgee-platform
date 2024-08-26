package io.tolgee.formats.csv.`in`

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class CsvFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {

  override fun process() {
    val inputStream = context.file.data.inputStream()
    val reader = inputStream.reader()
    val data: List<Array<String>>

    CSVReaderBuilder(reader)
      .withCSVParser(
        CSVParserBuilder()
          .withSeparator(';')     // TODO make delimiter parametrizable
          .build()
      ).build().use { csvReader -> data = csvReader.readAll() }


    val format = getFormat(data)

    // Read the first line to extract headers (language keys) - "key_name", ...languages
    val headers = data.firstOrNull()
    val languages = headers?.drop(1) ?: emptyList()

    // Parse body - key_name, ...translations
    for ((idx, row) in data.drop(1).withIndex()) {
      val keyName = row.getOrNull(0) ?: throw ImportCannotParseFileException(context.file.name, "empty row $idx")

      for (i in 1 until row.size) {
        val languageTag = languages.getOrNull(i - 1) ?: throw ImportCannotParseFileException(
          context.file.name, "more translations than defined languages in row $idx"
        )
        val translation = row.getOrNull(i) ?: throw ImportCannotParseFileException(
          context.file.name, "missing translation in row $idx"
        )

        val converted = format.messageConvertor.convert(
          translation, languageTag,
          convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
          isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled
        )
        context.addTranslation(
          keyName,
          languageTag,
          converted.message,
          idx,
          pluralArgName = converted.pluralArgName,
          rawData = row[i],
          convertedBy = format,
        )
      }
    }
  }

  private fun getFormat(data: List<Array<String>>): ImportFormat {
    return context.mapping?.format ?: CSVImportFormatDetector().detectFormat(data)
  }

}
