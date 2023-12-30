package io.tolgee.service.dataImport.processors.messageFormat

class FormatDetector(private val messages: List<String>) {
  /**
   * Tries to detect message format by on all messages in file
   */
  operator fun invoke(): SupportedFormat {
    val regulars =
      mapOf(
        SupportedFormat.C to ToICUConverter.C_PARAM_REGEX,
        SupportedFormat.PHP to ToICUConverter.PHP_PARAM_REGEX,
        SupportedFormat.PYTHON to ToICUConverter.PYTHON_PARAM_REGEX,
      )

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
