package io.tolgee.formats.json.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.ICU_DETECTION_REGEX
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.I18nextToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor

class JsonImportFormatDetector {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.JSON_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              JavaToIcuPlaceholderConvertor.JAVA_DETECTION_REGEX,
              // java is less probable than php
              0.9,
            ),
          ),
        ImportFormat.JSON_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PhpToIcuPlaceholderConvertor.PHP_DETECTION_REGEX,
            ),
          ),
        ImportFormat.JSON_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(ICU_DETECTION_REGEX),
          ),
        ImportFormat.JSON_RUBY to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              RubyToIcuPlaceholderConvertor.RUBY_DETECTION_REGEX,
              0.7,
            ),
          ),
        ImportFormat.JSON_C to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              CToIcuPlaceholderConvertor.C_DETECTION_REGEX,
              0.6,
            ),
          ),
        ImportFormat.JSON_I18NEXT to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              I18nextToIcuPlaceholderConvertor.I18NEXT_DETECTION_REGEX,
            ),
          ),
      )
  }

  fun detectFormat(data: Any?): ImportFormat {
    return detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.JSON_ICU
  }
}
