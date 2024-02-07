package io.tolgee.formats.po.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.exceptions.PoParserException
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.formats.po.`in`.data.PoParsedTranslation
import io.tolgee.formats.po.`in`.data.PoParserResult
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor

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
          val icuMessage =
            getToIcuConverter(poTranslation)
              .convert(poTranslation.msgstr.toString())
          context.addTranslation(keyName, languageId, icuMessage, idx)

          poTranslation.meta.references.forEach { reference ->
            val split = reference.split(":")
            val file = split.getOrNull(0)
            val line = split.getOrNull(1)?.toIntOrNull()
            file?.let {
              context.addKeyCodeReference(keyName, it, line?.toLong())
            }
          }
          if (poTranslation.meta.extractedComments.isNotEmpty()) {
            val extractedComments = poTranslation.meta.extractedComments.joinToString(" ")
            context.addKeyDescription(keyName, extractedComments)
          }

          if (poTranslation.meta.translatorComments.isNotEmpty()) {
            val translatorComments = poTranslation.meta.translatorComments.joinToString(" ")
            context.addKeyDescription(keyName, translatorComments)
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
      val icuMessage =
        PoToICUConverter(uLocale, getMessageFormat(poTranslation))
          .convertPoPlural(plurals)
      context.addTranslation(poTranslation.msgidPlural.toString(), languageId, icuMessage, idx, isPlural = true)
    }
  }

  private fun getToIcuConverter(poTranslation: PoParsedTranslation): PoToICUConverter {
    return PoToICUConverter(uLocale, getMessageFormat(poTranslation))
  }

  private fun getMessageFormat(poParsedTranslation: PoParsedTranslation): SupportedFormat {
    poParsedTranslation.meta.flags.forEach {
      SupportedFormat.findByFlag(it)
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

  private val uLocale by lazy {
    getULocaleFromTag(languageId)
  }
}
