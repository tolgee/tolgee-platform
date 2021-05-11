package io.tolgee.service.dataImport.processors.poProcessor.data

data class PoParserResult(
        val meta: PoParserMeta,
        val translations: List<PoParsedTranslation>
)
