package io.tolgee.formats.json.`in`

import io.tolgee.formats.android.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.ICU_DETECTION_REGEX
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor.Companion.PHP_PLACEHOLDER_REGEX

class JsonImportFormatDetector {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.JSON_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              JavaToIcuPlaceholderConvertor.JAVA_PLACEHOLDER_REGEX,
              // java is less probable than php
              0.9,
            ),
          ),
        ImportFormat.JSON_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PHP_PLACEHOLDER_REGEX,
            ),
          ),
        ImportFormat.JSON_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(ICU_DETECTION_REGEX),
          ),
      )
  }

  fun detectFormat(data: Any?): ImportFormat {
    return detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.JSON_ICU
  }
}
