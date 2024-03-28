package io.tolgee.formats.xliff.`in`

import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.ICU_DETECTION_REGEX
import io.tolgee.formats.genericStructuredFile.`in`.FormatDetectionUtil.detectFromPossibleFormats
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.formats.xliff.model.XliffModel

class XliffImportFormatDetector {
  companion object {
    private val possibleFormats =
      mapOf(
        ImportFormat.XLIFF_ICU to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              ICU_DETECTION_REGEX,
            ),
          ),
        ImportFormat.XLIFF_PHP to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              PhpToIcuPlaceholderConvertor.PHP_DETECTION_REGEX,
            ),
          ),
        ImportFormat.XLIFF_JAVA to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              JavaToIcuPlaceholderConvertor.JAVA_DETECTION_REGEX,
            ),
          ),
        ImportFormat.XLIFF_RUBY to
          arrayOf(
            FormatDetectionUtil.regexFactor(
              RubyToIcuPlaceholderConvertor.RUBY_DETECTION_REGEX,
              0.95,
            ),
          ),
      )
  }

  fun detectFormat(parsed: XliffModel): ImportFormat {
    val detectionData =
      parsed.files
        .flatMap {
          it.transUnits
            .flatMap { unit -> listOf(unit.source, unit.target) }
        }
    return detectFromPossibleFormats(possibleFormats, detectionData)
      ?: ImportFormat.XLIFF_ICU
  }
}
