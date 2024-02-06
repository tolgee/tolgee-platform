package io.tolgee.formats.xliff.model

class XliffFile {
  val transUnits = mutableListOf<XliffTransUnit>()
  var original: String? = null
  var sourceLanguage: String? = null
  var targetLanguage: String? = null
}
