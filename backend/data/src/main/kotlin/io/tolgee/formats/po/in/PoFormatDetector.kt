package io.tolgee.formats.po.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor.Companion.JAVA_PLACEHOLDER_REGEX
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor.Companion.PHP_PLACEHOLDER_REGEX

class PoFormatDetector() {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.PO_C to
          arrayOf(
            FormatDetectionUtil.regexFactor(CToIcuPlaceholderConvertor.C_PLACEHOLDER_REGEX),
          ),
        ImportFormat.PO_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              JAVA_PLACEHOLDER_REGEX,
              // gettext is not very popular in java world
              0.95,
            ),
          ),
        ImportFormat.PO_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PHP_PLACEHOLDER_REGEX,
              // gettext is not very popular in java world
              0.95,
            ),
          ),
        ImportFormat.PO_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PHP_PLACEHOLDER_REGEX,
              1.05,
            ),
          ),
//        ImportFormat.PO_PYTHON to
//          arrayOf(
//            FormatDetectionUtil.regexFactor(
//              PHP_PLACEHOLDER_REGEX,
//              1.05,
//            ),
//          ),
      )
  }

  fun detectByFlag(flag: String): ImportFormat? {
    return when (flag) {
      "php-format" -> return ImportFormat.PO_PHP
      "c-format" -> return ImportFormat.PO_C
      "java-format" -> return ImportFormat.PO_JAVA
      "icu-format" -> return ImportFormat.PO_ICU
      else -> null
    }
  }

  fun detectFormat(data: List<String>): ImportFormat {
    return FormatDetectionUtil.detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.PO_PHP
  }
}
