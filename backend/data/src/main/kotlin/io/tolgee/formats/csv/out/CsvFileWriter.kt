package io.tolgee.formats.csv.out

import com.opencsv.CSVWriterBuilder
import io.tolgee.formats.csv.CsvEntry
import java.io.InputStream
import java.io.StringWriter

class CsvFileWriter(
  private val languageTags: Array<String>,
  private val data: List<CsvEntry>,
  private val delimiter: Char,
) {
  val translations: Map<String, Map<String, String?>> by lazy {
    data.groupBy { it.key }.mapValues { (_, values) ->
      values.associate { it.language to it.value }
    }
  }

  fun produceFiles(): InputStream {
    val output = StringWriter()
    val writer = CSVWriterBuilder(output).withSeparator(delimiter).build()
    writer.writeNext(
      arrayOf("key") + languageTags,
    )
    translations.forEach {
      writer.writeNext(
        arrayOf(it.key) +
          languageTags.map { languageTag ->
            it.value.getOrDefault(languageTag, null) ?: ""
          }.toTypedArray(),
      )
    }
    return output.toString().byteInputStream()
  }
}
