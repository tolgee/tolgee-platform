package io.tolgee.formats

import io.tolgee.formats.android.out.IcuToJavaPlaceholderConvertor
import io.tolgee.formats.apple.out.IcuToApplePlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToCPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToPhpPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToRubyPlaceholderConvertor

enum class ExportMessageFormat(val paramConvertorFactory: () -> FromIcuPlaceholderConvertor) {
  C_SPRINTF(paramConvertorFactory = { IcuToCPlaceholderConvertor() }),
  PHP_SPRINTF(paramConvertorFactory = { IcuToPhpPlaceholderConvertor() }),
  JAVA_SPRINTF(paramConvertorFactory = { IcuToJavaPlaceholderConvertor() }),
  APPLE_SPRINTF(paramConvertorFactory = { IcuToApplePlaceholderConvertor() }),
  RUBY_SPRINTF(paramConvertorFactory = { IcuToRubyPlaceholderConvertor() }),
  ICU(paramConvertorFactory = { NoOpFromIcuPlaceholderConvertor() }),
//  PYTHON_SPRINTF,
}
