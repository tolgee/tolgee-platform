package io.tolgee.formats.xlsx.`in`

import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.`in`.TableParser
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class XlsxFileParser(
  private val inputStream: InputStream,
  private val languageFallback: String,
) {
  val rawData: List<List<List<String>>> by lazy {
    val workbook = WorkbookFactory.create(inputStream)
    return@lazy workbook
      .sheetIterator()
      .asSequence()
      .map {
        it.map { it.map { it.stringCellValue } }
      }.toList()
  }

  fun parse(): List<TableEntry> {
    return rawData.flatMap {
      TableParser(it, languageFallback).parse()
    }
  }
}
