package io.tolgee.formats.xlsx.out

import io.tolgee.formats.genericTable.TableEntry
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Date
import java.util.Optional

class XlsxFileWriter(
  private val createdDate: Date,
  private val languageTags: Array<String>,
  private val data: List<TableEntry>,
) {
  val translations: Map<String, Map<String, String?>> by lazy {
    data.groupBy { it.key }.mapValues { (_, values) ->
      values.associate { it.language to it.value }
    }
  }

  fun produceFiles(): InputStream {
    val output = ByteArrayOutputStream()
    val workbook = XSSFWorkbook()
    workbook.properties.coreProperties.setCreated(Optional.of(createdDate))
    val sheet = workbook.createSheet()

    val header = sheet.createRow(0)
    (listOf("key") + languageTags).forEachIndexed { i, v ->
      header.createCell(i).setCellValue(v)
    }

    translations.entries.forEachIndexed { i, it ->
      val row = sheet.createRow(i + 1)
      (
        listOf(it.key) +
          languageTags.map { languageTag ->
            it.value.getOrDefault(languageTag, null) ?: ""
          }
      ).forEachIndexed { i, v ->
        row.createCell(i).setCellValue(v)
      }
    }

    workbook.write(output)
    return output.toByteArray().inputStream()
  }
}
