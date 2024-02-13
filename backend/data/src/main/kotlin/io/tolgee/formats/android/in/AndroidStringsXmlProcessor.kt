package io.tolgee.formats.android.`in`

import AndroidStringsXmlParser
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.android.PluralUnit
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AndroidStringsXmlProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    val parsed = AndroidStringsXmlParser(xmlEventReader).parse()

    parsed.items.forEach { (keyName, item) ->
      when (item) {
        is StringUnit -> handleString(keyName, item)
        is PluralUnit -> handlePlural(keyName, item)
        is StringArrayUnit -> handleStringsArray(keyName, item)
        else -> {}
      }
    }
  }

  private fun handleString(
    keyName: String,
    it: StringUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    context.addTranslation(keyName, guessedLanguage, convertMessage(it.value, false), forceIsPlural = false)
  }

  private fun handlePlural(
    keyName: String,
    it: PluralUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    val forms =
      it.items.mapNotNull {
        val converted = convertMessage(it.value, true)
        it.key to (converted ?: return@mapNotNull null)
      }.toMap()

    val pluralString = FormsToIcuPluralConvertor(forms, escape = false).convert()
    context.addTranslation(keyName, guessedLanguage, pluralString, forceIsPlural = true)
  }

  private fun handleStringsArray(
    keyName: String,
    arrayUnit: StringArrayUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    arrayUnit.items.forEachIndexed { index, item ->
      val keyNameWithIndex = "$keyName[$index]"
      context.addTranslation(
        keyNameWithIndex,
        guessedLanguage,
        convertMessage(item.value, false),
        forceIsPlural = false,
      )
    }
  }

  private val guessedLanguage: String by lazy {
    val matchResult = LANGUAGE_GUESS_REGEX.find(context.file.name) ?: return@lazy "unknown"
    val language = matchResult.groups["language"]!!.value
    val region = matchResult.groups["region"]?.value
    val regionString = region?.let { "-$it" } ?: ""
    "$language$regionString"
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newInstance()
    inputFactory.createXMLEventReader(context.file.data.inputStream())
  }

  private fun convertMessage(
    message: String?,
    isPlural: Boolean,
  ): String? {
    message ?: return null
    return io.tolgee.formats.convertMessage(message, isPlural) {
      JavaToIcuParamConvertor()
    }
  }

  companion object {
    val LANGUAGE_GUESS_REGEX = Regex("values-(?<language>[a-zA-Z]{2,3})(-r(?<region>[a-zA-Z]{2,3}))?")
  }
}
