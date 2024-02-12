package io.tolgee.formats.android.`in`

import AndroidStringsXmlParser
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.android.StringsModel
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AndroidStringsXmlProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    val parsed = AndroidStringsXmlParser(xmlEventReader).parse()
    handleStrings(parsed)
    handlePlurals(parsed)
    handleStringsArray(parsed)
  }

  private fun handleStrings(parsed: StringsModel) {
    parsed.strings.forEach {
      if (it.name.isNullOrBlank())
        {
          return@forEach
        }
      context.addTranslation(it.name!!, guessedLanguage, convertMessage(it.value, false))
    }
  }

  private fun handlePlurals(parsed: StringsModel) {
    parsed.plurals.forEach { pluralsUnit ->
      val keyName = pluralsUnit.name ?: return@forEach
      if (keyName.isBlank())
        {
          return@forEach
        }

      val forms =
        pluralsUnit.items.mapNotNull {
          val converted = convertMessage(it.value, true)
          it.key to (converted ?: return@mapNotNull null)
        }.toMap()

      val pluralString = FormsToIcuPluralConvertor(forms, escape = false).convert()
      context.addTranslation(keyName, guessedLanguage, pluralString)
    }
  }

  private fun handleStringsArray(parsed: StringsModel) {
    parsed.stringArrays.forEach { arrayUnit ->
      if (arrayUnit.name.isNullOrBlank()) {
        return@forEach
      }
      arrayUnit.items.forEachIndexed { index, item ->
        val keyName = "${arrayUnit.name}[$index]"
        context.addTranslation(keyName, guessedLanguage, convertMessage(item, false))
      }
    }
  }

  private val guessedLanguage: String by lazy {
    val matchResult = LANGUAGE_GUESS_REGEX.find(context.file.name) ?: return@lazy "unknown"
    val language = matchResult.groups["language"]!!.value
    val region = matchResult.groups["region"]?.value
    "$language-$region"
  }

  private val xmlEventReader: XMLEventReader by lazy {
    val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
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
