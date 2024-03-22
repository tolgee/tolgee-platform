package io.tolgee.formats.importCommon

import io.tolgee.formats.paramConvertors.`in`.AppleToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.formats.po.`in`.PoToIcuMessageConvertor

enum class ImportFormat(
  val pluralsViaNesting: Boolean = false,
  val messageConvertorOrNull: ImportMessageConvertor? = null,
  val rootKeyIsLanguageTag: Boolean = false,
) {
  JSON_ICU(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  JSON_JAVA(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = { JavaToIcuPlaceholderConvertor() },
      ),
  ),
  JSON_PHP(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = { PhpToIcuPlaceholderConvertor() },
      ),
  ),

  PO_PHP(messageConvertorOrNull = PoToIcuMessageConvertor { CToIcuPlaceholderConvertor() }),
  PO_C(messageConvertorOrNull = PoToIcuMessageConvertor { PhpToIcuPlaceholderConvertor() }),
  PO_JAVA(messageConvertorOrNull = PoToIcuMessageConvertor { JavaToIcuPlaceholderConvertor() }),
  PO_ICU(messageConvertorOrNull = PoToIcuMessageConvertor(paramConvertorFactory = null)),
//  PO_PYTHON(messageConvertorOrNull = BasePoToIcuMessageConvertor { PythonToIcuPlaceholderConvertor() }),

  STRINGS(messageConvertorOrNull = appleConvertor),
  STRINGSDICT(messageConvertorOrNull = appleConvertor),
  APPLE_XLIFF(messageConvertorOrNull = appleConvertor),

  // properties don't store plurals in map, but it doesn't matter.
  // Since they don't support nesting at all, we cannot have plurals by nesting in them, so the plural extracting
  // code won't be executed
  PROPERTIES_ICU(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  PROPERTIES_JAVA(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  PROPERTIES_UNKNOWN(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor(toIcuPlaceholderConvertorFactory = null),
  ),

  ANDROID_XML(messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() }),

  FLUTTER_ARB(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),

  YAML_RUBY(
    pluralsViaNesting = true,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { RubyToIcuPlaceholderConvertor() },
    rootKeyIsLanguageTag = true,
  ),
  YAML_JAVA(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  YAML_ICU(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  YAML_UNKNOWN(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),

  XLIFF_ICU(
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),

  XLIFF_JAVA(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),

  XLIFF_PHP(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { PhpToIcuPlaceholderConvertor() },
  ),

  XLIFF_RUBY(
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { RubyToIcuPlaceholderConvertor() },
  ),

  ;

  val messageConvertor: ImportMessageConvertor
    get() =
      this.messageConvertorOrNull
        ?: throw IllegalStateException(
          "Message convertor required. Provide message convertor for " +
            "${ImportFormat::class.simpleName}.${this.name}",
        )
}

private val appleConvertor =
  GenericMapPluralImportRawDataConvertor(optimizePlurals = true) { AppleToIcuPlaceholderConvertor() }
