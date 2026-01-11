package io.tolgee.formats

class PossiblePluralConversionResult(
  val singleResult: String? = null,
  val formsResult: Map<String, String>? = null,
  val argName: String? = null,
  val firstArgName: String? = null,
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
