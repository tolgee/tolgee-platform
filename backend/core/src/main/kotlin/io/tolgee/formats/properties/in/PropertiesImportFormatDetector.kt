package io.tolgee.formats.properties.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor

class PropertiesImportFormatDetector {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.PROPERTIES_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(JavaToIcuPlaceholderConvertor.JAVA_DETECTION_REGEX),
          ),
        ImportFormat.PROPERTIES_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(FormatDetectionUtil.ICU_DETECTION_REGEX),
          ),
      )
  }

  fun detectFormat(data: Map<*, *>): ImportFormat {
    return detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.PROPERTIES_UNKNOWN
  }
}
