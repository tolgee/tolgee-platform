package io.tolgee.formats.po.`in`

import io.tolgee.formats.po.SupportedFormat

class FormatDetector(private val messages: List<String>) {
  /**
   * Tries to detect message format by on all messages in file
   */
  operator fun invoke(): SupportedFormat {
    val regulars =
      SupportedFormat.entries.associateWith { it.paramConvertorFactory().regex }

    val hitsMap = mutableMapOf<SupportedFormat, Int>()
    regulars.forEach { regularEntry ->
      val format = regularEntry.key
      val regex = regularEntry.value
      messages.forEach {
        hitsMap[format] = (hitsMap[format] ?: 0) + regex.findAll(it).count()
      }
    }

    var result = SupportedFormat.PHP
    var maxValue = 0

    hitsMap.forEach {
      if (it.value > maxValue) {
        maxValue = it.value
        result = it.key
      }
    }

    return result
  }
}
