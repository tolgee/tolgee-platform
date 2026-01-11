package io.tolgee.formats.importCommon

fun unwrapString(rawData: Any?): String? {
  return rawData as? String ?: (rawData as? Map<*, *>)?.get(STRING_WRAPPER_VALUE_ITEM) as? String
}

private const val STRING_WRAPPER_VALUE_ITEM = "_stringValue"

fun Any?.wrapIfRequired(): Any? {
  if (this is String) {
    return mapOf(STRING_WRAPPER_VALUE_ITEM to this)
  }
  return this
}
