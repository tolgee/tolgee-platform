package io.tolgee.formats.yaml.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.ICU_DETECTION_REGEX
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor

class YamlImportFormatDetector {
  companion object {
    private val singleKeyIsBcp47TagFactor =
      FormatDetectionUtil.Factor(5.0) {
        if (it is Map<*, *> && it.size == 1) {
          val key = it.keys.first()
          if (FormatDetectionUtil.isValidBCP47Tag(key.toString())) {
            return@Factor 1.0
          }
        }
        return@Factor 0.0
      }

    private val possibleFormats =
      mapOf(
        ImportFormat.YAML_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(JavaToIcuPlaceholderConvertor.JAVA_DETECTION_REGEX),
          ),
        ImportFormat.YAML_RUBY to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              RubyToIcuPlaceholderConvertor.RUBY_DETECTION_REGEX,
              // Ruby is much more specific than java, so we can lower it's weights.
              // If the format is candidate for RUBY and JAVA at the same time, it will be detected as JAVA
              0.9,
            ),
            singleKeyIsBcp47TagFactor,
          ),
        ImportFormat.YAML_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(ICU_DETECTION_REGEX),
          ),
        ImportFormat.YAML_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PhpToIcuPlaceholderConvertor.PHP_DETECTION_REGEX,
              // java is less probable than php
              0.7,
            ),
          ),
      )
  }

  fun detectFormat(data: Map<*, *>): ImportFormat {
    return detectFromPossibleFormats(possibleFormats, data) ?: ImportFormat.YAML_UNKNOWN
  }
}
