package io.tolgee.service.dataImport.processors.po.data

data class PoParserResult(
  val meta: PoParserMeta,
  val translations: List<PoParsedTranslation>,
)
