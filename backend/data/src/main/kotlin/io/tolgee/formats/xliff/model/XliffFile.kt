package io.tolgee.formats.xliff.model

class XliffFile {
  val transUnits = mutableListOf<XliffTransUnit>()
  var original: String? = null
  var sourceLanguage: String? = null
  var targetLanguage: String? = null
  val datatype: String = "plaintext"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as XliffFile

    if (original != other.original) return false
    if (sourceLanguage != other.sourceLanguage) return false
    if (targetLanguage != other.targetLanguage) return false

    return true
  }

  override fun hashCode(): Int {
    var result = sourceLanguage?.hashCode() ?: 0
    result = 31 * result + (targetLanguage?.hashCode() ?: 0)
    return result
  }
}
