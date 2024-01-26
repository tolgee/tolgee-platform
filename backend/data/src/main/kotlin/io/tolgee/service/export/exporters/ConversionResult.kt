package io.tolgee.service.export.exporters

import io.tolgee.constants.Message

class ConversionResult(
  val result: String?,
  val forms: List<String>?,
  val warnings: List<Pair<Message, List<Any>>>,
) {
  init {
    if (result == null && forms == null) {
      throw IllegalArgumentException("Both result and forms cannot be null")
    }
  }

  fun isPlural(): Boolean {
    return forms != null
  }
}
