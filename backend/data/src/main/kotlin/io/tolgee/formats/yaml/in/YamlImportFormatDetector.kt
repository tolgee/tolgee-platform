package io.tolgee.formats.yaml.`in`

import io.tolgee.formats.android.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importMessageFormat.ImportMessageFormat
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor

class YamlImportFormatDetector {
  companion object {
    private val singleKeyIsBcp47TagFactor =
      FormatDetectionUtil.Factor(5.0) {
        if (it.size == 1) {
          val key = it.keys.first()
          if (FormatDetectionUtil.isValidBCP47Tag(key.toString())) {
            return@Factor 1.0
          }
        }
        return@Factor 0.0
      }

    private val possibleFormats =
      mapOf(
        ImportMessageFormat.YAML_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(JavaToIcuPlaceholderConvertor.JAVA_PLACEHOLDER_REGEX),
          ),
        ImportMessageFormat.YAML_RUBY to
          arrayOf(
            FormatDetectionUtil.regexFactor(RubyToIcuPlaceholderConvertor.RUBY_PLACEHOLDER_REGEX),
            singleKeyIsBcp47TagFactor,
          ),
        ImportMessageFormat.YAML_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor("\\s\\{\\w+\\}\\W".toRegex()),
          ),
      )
  }

  fun detectFormat(data: Map<*, *>): ImportMessageFormat {
    return detectFromPossibleFormats(possibleFormats, data) ?: ImportMessageFormat.UNKNOWN
  }
}
