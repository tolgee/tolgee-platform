package io.tolgee.formats.po.`in`.data

data class PoParserResult(
  val meta: PoParserMeta,
  val translations: List<PoParsedTranslation>,
)
