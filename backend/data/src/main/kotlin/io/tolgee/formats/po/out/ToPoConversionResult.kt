package io.tolgee.formats.po.out

import io.tolgee.constants.Message

class ToPoConversionResult(
  val singleResult: String?,
  val formsResult: List<String>?,
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
