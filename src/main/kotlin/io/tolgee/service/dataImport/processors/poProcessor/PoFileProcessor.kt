package io.tolgee.service.dataImport.processors.poProcessor

import com.ibm.icu.util.ULocale
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.PoParserException
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import io.tolgee.service.dataImport.processors.messageFormat.SupportedFormat
import io.tolgee.service.dataImport.processors.messageFormat.ToICUConverter
import io.tolgee.service.dataImport.processors.poProcessor.data.PoParsedTranslation
import io.tolgee.service.dataImport.processors.poProcessor.data.PoParserResult

class PoFileProcessor(
        override val context: FileProcessorContext
) : ImportFileProcessor() {

    lateinit var languageId: String
    lateinit var parsed: PoParserResult
    var poParser: PoParser = PoParser(context)

    override fun process() {
        try {
            parsed = poParser()
            languageId = parsed.meta.language ?: languageNameGuesses[0]
            context.languages[languageId] = ImportLanguage(languageId, context.fileEntity)

            parsed.translations.forEach { poTranslation ->
                if (poTranslation.msgidPlural.isNotEmpty()) {
                    addPlural(poTranslation)
                    return@forEach
                }
                if (poTranslation.msgid.isNotBlank() && poTranslation.msgstr.isNotBlank()) {
                    val icuMessage = ToICUConverter(ULocale(languageId), getMessageFormat(poTranslation, parsed))
                            .convert(poTranslation.msgstr.toString())
                    context.addTranslation(poTranslation.msgid.toString(), languageId, icuMessage)
                }
            }

        } catch (e: PoParserException) {
            throw ImportCannotParseFileException(context.file.name, e.message)
        }
    }

    private fun addPlural(poTranslation: PoParsedTranslation) {
        val plurals = poTranslation.msgstrPlurals?.map { it.key to it.value.toString() }?.toMap()
        plurals?.let {
            val icuMessage = ToICUConverter(ULocale(languageId), getMessageFormat(poTranslation, parsed))
                    .convertPoPlural(plurals)
            context.addTranslation(poTranslation.msgidPlural.toString(), languageId, icuMessage)
        }
    }

    private fun getMessageFormat(
            poParsedTranslation: PoParsedTranslation,
            poParserResult: PoParserResult
    ): SupportedFormat {
        poParsedTranslation.meta.flags.forEach {
            SupportedFormat.findByFlag(it)
                    ?.let { found -> return found }
        }
        return detectFormat(poParsedTranslation, poParserResult)
    }

    private fun detectFormat(
            poParsedTranslation: PoParsedTranslation,
            poParserResult: PoParserResult
    ): SupportedFormat {
        return SupportedFormat.PHP
    }
}

