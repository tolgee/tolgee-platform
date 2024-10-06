package io.tolgee.formats.importCommon

import io.tolgee.formats.paramConvertors.`in`.AppleToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.I18nextToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.JavaToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.formats.po.`in`.PoToIcuMessageConvertor

enum class ImportFormat(
  val fileFormat: ImportFileFormat,
  val pluralsViaNesting: Boolean = false,
  val pluralsViaSuffixesParser: PluralsKeyParser? = null,
  val messageConvertorOrNull: ImportMessageConvertor? = null,
  val rootKeyIsLanguageTag: Boolean = false,
) {
  CSV_ICU(
    ImportFileFormat.CSV,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  CSV_JAVA(
    ImportFileFormat.CSV,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  CSV_PHP(
    ImportFileFormat.CSV,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { PhpToIcuPlaceholderConvertor() },
  ),
  CSV_RUBY(
    ImportFileFormat.CSV,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { RubyToIcuPlaceholderConvertor() },
  ),

  JSON_I18NEXT(
    ImportFileFormat.JSON,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { I18nextToIcuPlaceholderConvertor() },
    pluralsViaSuffixesParser = I18nextToIcuPlaceholderConvertor.I18NEXT_PLURAL_SUFFIX_KEY_PARSER,
  ),
  JSON_ICU(
    ImportFileFormat.JSON,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  JSON_JAVA(
    ImportFileFormat.JSON,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = { JavaToIcuPlaceholderConvertor() },
      ),
  ),
  JSON_PHP(
    ImportFileFormat.JSON,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = { PhpToIcuPlaceholderConvertor() },
      ),
  ),
  JSON_RUBY(
    ImportFileFormat.JSON,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = { RubyToIcuPlaceholderConvertor() },
      ),
  ),
  JSON_C(
    ImportFileFormat.JSON,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = { CToIcuPlaceholderConvertor() },
      ),
  ),

  PO_PHP(
    ImportFileFormat.PO,
    messageConvertorOrNull = PoToIcuMessageConvertor { CToIcuPlaceholderConvertor() },
  ),
  PO_C(
    ImportFileFormat.PO,
    messageConvertorOrNull = PoToIcuMessageConvertor { PhpToIcuPlaceholderConvertor() },
  ),
  PO_JAVA(
    ImportFileFormat.PO,
    messageConvertorOrNull = PoToIcuMessageConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  PO_ICU(
    ImportFileFormat.PO,
    messageConvertorOrNull =
      PoToIcuMessageConvertor(
        canContainIcu = true,
        paramConvertorFactory = null,
      ),
  ),
  PO_RUBY(
    ImportFileFormat.PO,
    messageConvertorOrNull = PoToIcuMessageConvertor { RubyToIcuPlaceholderConvertor() },
  ),
//  PO_PYTHON(messageConvertorOrNull = BasePoToIcuMessageConvertor { PythonToIcuPlaceholderConvertor() }),

  STRINGS(
    ImportFileFormat.STRINGS,
    messageConvertorOrNull = appleConvertor,
  ),
  STRINGSDICT(
    ImportFileFormat.STRINGSDICT,
    messageConvertorOrNull = appleConvertor,
  ),
  APPLE_XLIFF(
    ImportFileFormat.XLIFF,
    messageConvertorOrNull = appleConvertor,
  ),

  // properties don't store plurals in map, but it doesn't matter.
  // Since they don't support nesting at all, we cannot have plurals by nesting in them, so the plural extracting
  // code won't be executed
  PROPERTIES_ICU(
    ImportFileFormat.PROPERTIES,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  PROPERTIES_JAVA(
    ImportFileFormat.PROPERTIES,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  PROPERTIES_UNKNOWN(
    ImportFileFormat.PROPERTIES,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor(toIcuPlaceholderConvertorFactory = null),
  ),

  ANDROID_XML(
    ImportFileFormat.XML,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),

  FLUTTER_ARB(
    ImportFileFormat.ARB,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  YAML_RUBY(
    ImportFileFormat.YAML,
    pluralsViaNesting = true,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { RubyToIcuPlaceholderConvertor() },
    rootKeyIsLanguageTag = true,
  ),
  YAML_JAVA(
    ImportFileFormat.YAML,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),
  YAML_ICU(
    ImportFileFormat.YAML,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),
  YAML_PHP(
    ImportFileFormat.YAML,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor {
        PhpToIcuPlaceholderConvertor()
      },
  ),
  YAML_UNKNOWN(
    ImportFileFormat.YAML,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),

  XLIFF_ICU(
    ImportFileFormat.XLIFF,
    messageConvertorOrNull =
      GenericMapPluralImportRawDataConvertor(
        canContainIcu = true,
        toIcuPlaceholderConvertorFactory = null,
      ),
  ),

  XLIFF_JAVA(
    ImportFileFormat.XLIFF,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { JavaToIcuPlaceholderConvertor() },
  ),

  XLIFF_PHP(
    ImportFileFormat.XLIFF,
    messageConvertorOrNull = GenericMapPluralImportRawDataConvertor { PhpToIcuPlaceholderConvertor() },
  ),

  XLIFF_RUBY(
    ImportFileFormat.XLIFF,
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
