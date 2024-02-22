package io.tolgee.formats.po.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.PoParserException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.ImportMessageConvertorType
import io.tolgee.formats.StringWrapper
import io.tolgee.formats.po.PoSupportedMessageFormat
import io.tolgee.formats.po.`in`.data.PoParsedTranslation
import io.tolgee.formats.po.`in`.data.PoParserResult
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.util.nullIfEmpty

class PoFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  lateinit var languageId: String
  lateinit var parsed: PoParserResult
  var poParser: PoParser = PoParser(context)

  override fun process() {
    try {
      parsed = poParser()
      languageId = parsed.meta.language ?: languageNameGuesses[0]
      context.languages[languageId] = ImportLanguage(languageId, context.fileEntity)

      parsed.translations.forEachIndexed { idx, poTranslation ->
        val keyName = poTranslation.msgid.toString()

        if (poTranslation.msgidPlural.isNotEmpty()) {
          addPlural(poTranslation, idx)
          return@forEachIndexed
        }
        if (poTranslation.msgid.isNotBlank()) {
          val converted = getConvertedMessage(poTranslation, poTranslation.msgstr.toString())
          context.addTranslation(
            keyName = keyName,
            languageName = languageId,
            value = converted.first,
            idx = idx,
            rawData = StringWrapper(poTranslation.msgstr.toString()),
            convertedBy = converted.second,
          )

          poTranslation.meta.references.forEach { reference ->
            val split = reference.split(":")
            val file = split.getOrNull(0)
            val line = split.getOrNull(1)?.toIntOrNull()
            file?.let {
              context.addKeyCodeReference(keyName, it, line?.toLong())
            }
          }

          // we use only extracted comments. Translator comments should stay in tolgee and are useless for export
          if (poTranslation.meta.extractedComments.isNotEmpty()) {
            val extractedComments = poTranslation.meta.extractedComments.joinToString(" ")
            context.addKeyDescription(keyName, extractedComments)
          }
        }
      }
    } catch (e: PoParserException) {
      throw ImportCannotParseFileException(context.file.name, e.message)
    }
  }

  private fun addPlural(
    poTranslation: PoParsedTranslation,
    idx: Int,
  ) {
    val plurals = poTranslation.msgstrPlurals?.map { it.key to it.value.toString() }?.toMap()
    plurals?.let {
      val (message, convertedBy) = getConvertedMessage(poTranslation, plurals)
      val keyName = poTranslation.msgidPlural.toString().nullIfEmpty ?: poTranslation.msgid.toString()
      context.addTranslation(keyName, languageId, message, idx, rawData = plurals, convertedBy = convertedBy)
    }
  }

  private fun getConvertedMessage(
    poTranslation: PoParsedTranslation,
    stringOrPluralForms: Any?,
  ): Pair<String?, ImportMessageConvertorType?> {
    val messageFormat = getMessageFormat(poTranslation)
    val convertor = messageFormat.importMessageConvertorType.importMessageConvertor ?: return (null to null)
    val icuMessage =
      convertor.convert(stringOrPluralForms, languageId, context.importSettings.convertPlaceholdersToIcu).message
    return icuMessage to messageFormat.importMessageConvertorType
  }

  private fun getMessageFormat(poParsedTranslation: PoParsedTranslation): PoSupportedMessageFormat {
    poParsedTranslation.meta.flags.forEach {
      PoSupportedMessageFormat.findByFlag(it)
        ?.let { found -> return found }
    }
    return detectedFormat
  }

  private val detectedFormat by lazy {
    val messages =
      parsed.translations.flatMap { poParsed ->
        if (poParsed.msgidPlural.isNotBlank() && !poParsed.msgstrPlurals.isNullOrEmpty()) {
          poParsed.msgstrPlurals!!.values.asSequence().map { it.toString() }
        } else {
          sequence {
            yield(poParsed.msgstr.toString())
          }
        }
      }

    FormatDetector(messages.toList())()
  }
}
