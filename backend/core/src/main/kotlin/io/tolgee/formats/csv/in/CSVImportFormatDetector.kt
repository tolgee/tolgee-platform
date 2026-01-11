package io.tolgee.formats.csv.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.ICU_DETECTION_REGEX
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor

class CSVImportFormatDetector {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.CSV_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              ICU_DETECTION_REGEX,
            ),
          ),
        ImportFormat.CSV_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PhpToIcuPlaceholderConvertor.PHP_DETECTION_REGEX,
            ),
          ),
        ImportFormat.CSV_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              JavaToIcuPlaceholderConvertor.JAVA_DETECTION_REGEX,
            ),
          ),
        ImportFormat.CSV_RUBY to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              RubyToIcuPlaceholderConvertor.RUBY_DETECTION_REGEX,
              0.95,
            ),
          ),
      )
  }

  fun detectFormat(data: Any?): ImportFormat {
    return detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.CSV_ICU
  }
}
