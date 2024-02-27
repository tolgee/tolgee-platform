package io.tolgee.formats.po.`in`

import io.tolgee.formats.po.PoSupportedMessageFormat

class FormatDetector(private val messages: List<String>) {
  /**
   * Tries to detect message format by on all messages in file
   */
  operator fun invoke(): PoSupportedMessageFormat {
    val regulars =
      PoSupportedMessageFormat.entries.associateWith { it.paramRegex }

    val hitsMap = mutableMapOf<PoSupportedMessageFormat, Int>()
    regulars.forEach { regularEntry ->
      val format = regularEntry.key
      val regex = regularEntry.value
      messages.forEach {
        hitsMap[format] = (hitsMap[format] ?: 0) + regex.findAll(it).count()
      }
    }

    var result = PoSupportedMessageFormat.PHP
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
