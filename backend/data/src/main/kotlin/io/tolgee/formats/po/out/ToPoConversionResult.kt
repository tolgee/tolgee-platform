package io.tolgee.formats.po.out

class ToPoConversionResult(
  val singleResult: String?,
  val formsResult: List<String>?,
) {
  init {
    if (singleResult == null && formsResult == null) {
      throw IllegalArgumentException("Both result and forms cannot be null")
    }
  }

  fun isPlural(): Boolean {
    return formsResult != null
  }
}
