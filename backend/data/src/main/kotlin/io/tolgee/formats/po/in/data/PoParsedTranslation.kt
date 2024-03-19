package io.tolgee.formats.po.`in`.data

class PoParsedTranslation {
  var msgid: StringBuilder = StringBuilder()
  var msgidPlural: StringBuilder = StringBuilder()
  var msgstr: StringBuilder = StringBuilder()
  var msgstrPlurals: MutableMap<Int, StringBuilder>? = null
  var meta: PoTranslationMeta = PoTranslationMeta()
  var raw: StringBuilder = StringBuilder()

  fun addToPlurals(
    variant: Int,
    value: String,
  ) {
    val map = msgstrPlurals ?: mutableMapOf()
    msgstrPlurals = map
    val builder =
      map[variant] ?: let {
        val newBuilder = StringBuilder()
        map[variant] = newBuilder
        newBuilder
      }
    builder.append(value)
  }
}
