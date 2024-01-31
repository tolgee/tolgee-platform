package io.tolgee.formats.po.`in`.data

class PoTranslationMeta {
  var translatorComments: MutableList<String> = mutableListOf()
  var extractedComments: MutableList<String> = mutableListOf()
  var references: MutableList<String> = mutableListOf()
  var flags: MutableList<String> = mutableListOf()
  var context: MutableList<String> = mutableListOf()

  override fun toString(): String {
    return """PoTranslationMeta(translatorComments=$translatorComments, 
            |extractedComments=$extractedComments, reference=$references, flags=$flags)
      """.trimMargin()
  }
}
