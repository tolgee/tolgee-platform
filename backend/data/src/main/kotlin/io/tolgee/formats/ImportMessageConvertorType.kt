package io.tolgee.formats

import io.tolgee.formats.android.`in`.AndroidToIcuMessageConvertor
import io.tolgee.formats.apple.`in`.AppleToIcuMessageConvertor
import io.tolgee.formats.po.`in`.messageConvertors.PoCToIcuImportMessageConvertor
import io.tolgee.formats.po.`in`.messageConvertors.PoPhpToIcuImportMessageConvertor
import io.tolgee.formats.po.`in`.messageConvertors.PoPythonToIcuImportMessageConvertor

enum class ImportMessageConvertorType(
  val importMessageConvertor: ImportMessageConvertor? = null,
) {
  JSON,
  PO_PHP(PoPhpToIcuImportMessageConvertor()),
  PO_C(PoCToIcuImportMessageConvertor()),
  PO_PYTHON(PoPythonToIcuImportMessageConvertor()),
  STRINGS(AppleToIcuMessageConvertor()),
  STRINGSDICT(AppleToIcuMessageConvertor()),
  APPLE_XLIFF(AppleToIcuMessageConvertor()),
  XLIFF12,
  PROPERTIES,
  ANDROID_XML(AndroidToIcuMessageConvertor()),
  FLUTTER_ARB,
}
