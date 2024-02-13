package io.tolgee.formats

import io.tolgee.constants.Message

class PossiblePluralConversionResult(
  val singleResult: String? = null,
  val formsResult: Map<String, String>? = null,
  val argName: String? = null,
  val warnings: List<Pair<Message, List<Any>>> = emptyList(),
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
