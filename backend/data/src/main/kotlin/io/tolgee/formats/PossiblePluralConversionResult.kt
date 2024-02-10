package io.tolgee.formats

import io.tolgee.constants.Message

class PossiblePluralConversionResult(
  val singleResult: String?,
  val formsResult: Map<String, String>?,
  val argName: String?,
  val isWholeStringWrappedInPlural: Boolean,
  val warnings: List<Pair<Message, List<Any>>>,
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
