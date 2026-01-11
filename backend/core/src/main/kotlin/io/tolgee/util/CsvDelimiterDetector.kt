package io.tolgee.util

import java.io.InputStream

class CsvDelimiterDetector(
  private val inputStream: InputStream,
) {
  companion object {
    val DELIMITERS = listOf(',', ';', '\t')
  }

  val delimiter by lazy {
    val headerLine =
      inputStream.use {
        it
          .reader()
          .buffered()
          .lineSequence()
          .firstOrNull() ?: ""
      }
    val counts =
      DELIMITERS.map { delimiter ->
        headerLine.count { it == delimiter }
      }
    val bestIndex =
      counts.foldIndexed(0) { index, maxIndex, value ->
        val maxValue = counts[maxIndex]
        index.takeIf { value > maxValue } ?: maxIndex
      }
    DELIMITERS[bestIndex]
  }
}
