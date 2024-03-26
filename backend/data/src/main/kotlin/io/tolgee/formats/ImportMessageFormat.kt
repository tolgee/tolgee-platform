package io.tolgee.formats

import io.tolgee.formats.android.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.importMessageFormatConvertor.MessageFormatImportDataConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor

enum class ImportMessageFormat(
  val pluralsViaNesting: Boolean = false,
  val canContainIcu: Boolean = false,
  val placeholderConvertorFactory: (() -> ToIcuPlaceholderConvertor)?,
  val rootKeyIsLanguageTag: Boolean = false,
  val importDataConvertorFactory: (() -> MessageFormatImportDataConvertor)? = null,
) {
  YAML_RUBY(
    pluralsViaNesting = true,
    canContainIcu = false,
    placeholderConvertorFactory = { RubyToIcuPlaceholderConvertor() },
    rootKeyIsLanguageTag = true,
  ),
  YAML_JAVA(
    pluralsViaNesting = false,
    canContainIcu = false,
    placeholderConvertorFactory = { JavaToIcuPlaceholderConvertor() },
  ),
  YAML_ICU(
    pluralsViaNesting = false,
    canContainIcu = true,
    placeholderConvertorFactory = null,
  ),
  UNKNOWN(
    pluralsViaNesting = false,
    canContainIcu = true,
    placeholderConvertorFactory = null,
  ),
}
