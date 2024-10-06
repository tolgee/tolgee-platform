package io.tolgee.formats

import io.tolgee.formats.paramConvertors.out.IcuToApplePlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToCPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToI18nextPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToJavaPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToPhpPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToRubyPlaceholderConvertor

@Suppress("unused") // it's exposed to the API
enum class ExportMessageFormat(val paramConvertorFactory: () -> FromIcuPlaceholderConvertor) {
  C_SPRINTF(paramConvertorFactory = { IcuToCPlaceholderConvertor() }),
  PHP_SPRINTF(paramConvertorFactory = { IcuToPhpPlaceholderConvertor() }),
  JAVA_STRING_FORMAT(paramConvertorFactory = { IcuToJavaPlaceholderConvertor() }),
  APPLE_SPRINTF(paramConvertorFactory = { IcuToApplePlaceholderConvertor() }),
  RUBY_SPRINTF(paramConvertorFactory = { IcuToRubyPlaceholderConvertor() }),
  I18NEXT(paramConvertorFactory = { IcuToI18nextPlaceholderConvertor() }),
  ICU(paramConvertorFactory = { IcuToIcuPlaceholderConvertor() }),
//  PYTHON_SPRINTF,
}
