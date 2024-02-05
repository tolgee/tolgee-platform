package io.tolgee.formats.xliff.`in`.parser

class XliffParserResultFile {
  val transUnits = mutableListOf<XliffParserResultTransUnit>()
  var original: String? = null
  var sourceLanguage: String? = null
  var targetLanguage: String? = null
}
