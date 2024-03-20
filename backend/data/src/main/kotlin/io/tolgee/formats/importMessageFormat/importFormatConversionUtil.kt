package io.tolgee.formats.importMessageFormat

fun unwrapString(rawData: Any?): String? {
  return rawData as? String ?: (rawData as? Map<*, *>)?.get("_stringValue") as? String
}
