package io.tolgee.service.dataImport.processors.messageFormat.data

data class PluralLanguage(
  val tag: String,
  val name: String,
  val examples: List<PluralExample>,
  val nplurals: Int,
  val pluralsText: String,
  val pluralsFunc: (n: Int) -> Int,
)
