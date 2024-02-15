package io.tolgee.formats

import io.tolgee.formats.po.`in`.messageConverters.PoCToIcuMessageConverter
import io.tolgee.formats.po.`in`.messageConverters.PoPhpToIcuMessageConverter
import io.tolgee.formats.po.`in`.messageConverters.PoPythonToIcuMessageConverter

enum class MessageConvertorType(
  val messageConvertor: MessageConvertor? = null,
) {
  JSON,
  PO_PHP(PoPhpToIcuMessageConverter()),
  PO_C(PoCToIcuMessageConverter()),
  PO_PYTHON(PoPythonToIcuMessageConverter()),
  STRINGS,
  STRINGSDICT,
  APPLE_XLIFF,
  XLIFF12,
  PROPERTIES,
  ANDROID_XML,
  FLUTTER_ARB,
}
