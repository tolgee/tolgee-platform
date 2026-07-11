package io.tolgee.formats.po.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor.Companion.JAVA_DETECTION_REGEX
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PythonBraceToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PythonToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor

class PoFormatDetector {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.PO_C to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              CToIcuPlaceholderConvertor.C_DETECTION_REGEX,
            ),
          ),
        ImportFormat.PO_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              JAVA_DETECTION_REGEX,
              // gettext is not very popular in java world
              0.95,
            ),
          ),
        ImportFormat.PO_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              FormatDetectionUtil.ICU_DETECTION_REGEX,
              // gettext is not very popular in java world
              0.95,
            ),
          ),
        ImportFormat.PO_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PhpToIcuPlaceholderConvertor.PHP_DETECTION_REGEX,
              1.05,
            ),
          ),
        ImportFormat.PO_RUBY to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              RubyToIcuPlaceholderConvertor.RUBY_DETECTION_REGEX,
              0.7,
            ),
          ),
        ImportFormat.PO_PYTHON to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PythonToIcuPlaceholderConvertor.PYTHON_DETECTION_REGEX,
              1.0,
            ),
          ),
        ImportFormat.PO_PYTHON_BRACE to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PythonBraceToIcuPlaceholderConvertor.PYTHON_BRACE_DETECTION_REGEX,
              // A plain "{name}" matches both ICU and PYTHON_BRACE.
              // Weight below ICU, since ICU is preferred.
              0.9,
            ),
          ),
      )
  }

  fun detectByFlag(flag: String): ImportFormat? {
    return when (flag) {
      "php-format" -> ImportFormat.PO_PHP
      "c-format" -> ImportFormat.PO_C
      "java-format" -> ImportFormat.PO_JAVA
      "icu-format" -> ImportFormat.PO_ICU
      "ruby-format" -> ImportFormat.PO_RUBY
      "python-format" -> ImportFormat.PO_PYTHON
      "python-brace-format" -> ImportFormat.PO_PYTHON_BRACE
      else -> null
    }
  }

  fun detectFormat(data: List<String>): ImportFormat {
    return FormatDetectionUtil.detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.PO_PHP
  }
}
