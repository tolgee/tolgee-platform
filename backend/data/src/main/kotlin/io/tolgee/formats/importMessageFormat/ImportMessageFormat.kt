package io.tolgee.formats.importMessageFormat

import io.tolgee.formats.android.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.apple.`in`.AppleToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.formats.po.`in`.messageConvertors.PoCToIcuImportMessageConvertor
import io.tolgee.formats.po.`in`.messageConvertors.PoPhpToIcuImportMessageConvertor

enum class ImportMessageFormat(
  val pluralsViaNesting: Boolean = false,
  val canContainIcu: Boolean = false,
  val messageConvertorOrNull: ImportMessageConvertor? = null,
  val rootKeyIsLanguageTag: Boolean = false,
) {
  JSON(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  PO_PHP(messageConvertorOrNull = PoPhpToIcuImportMessageConvertor()),
  PO_C(messageConvertorOrNull = PoCToIcuImportMessageConvertor()),

  //  PO_PYTHON(PoPythonToIcuImportMessageConvertor()),
  STRINGS(messageConvertorOrNull = appleConvertor),
  STRINGSDICT(messageConvertorOrNull = appleConvertor),
  APPLE_XLIFF(messageConvertorOrNull = appleConvertor),

  // properties don't store plurals in map, but it doesn't matter.
  // Since they don't support nesting at all, we cannot have plurals by nesting in them, so the plural extracting
  // code won't be executed
  PROPERTIES(messageConvertorOrNull = GenericMapPluralImportRawDataConvertor(toIcuPlaceholderConvertorFactory = null)),
  JAVA_PROPERTIES(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),

  ANDROID_XML(messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() }),

  YAML_RUBY(
    pluralsViaNesting = true,
    canContainIcu = false,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { RubyToIcuPlaceholderConvertor() },
    rootKeyIsLanguageTag = true,
  ),
  YAML_JAVA(
    pluralsViaNesting = false,
    canContainIcu = false,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  YAML_ICU(
    pluralsViaNesting = false,
    canContainIcu = true,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  UNKNOWN(
    pluralsViaNesting = false,
    canContainIcu = true,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),

  ;

  val messageConvertor: ImportMessageConvertor
    get() =
      this.messageConvertorOrNull
        ?: throw IllegalStateException(
          "Message convertor required. Provide message convertor for " +
            "${ImportMessageFormat::class.simpleName}.${this.name}",
        )
}

private val appleConvertor =
  GenericMapPluralImportRawDataConvertor(optimizePlurals = true) { AppleToIcuPlaceholderConvertor() }
