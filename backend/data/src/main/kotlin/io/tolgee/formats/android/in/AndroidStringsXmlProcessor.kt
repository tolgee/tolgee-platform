package io.tolgee.formats.android.`in`

import AndroidStringsXmlParser
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.android.ANDROID_CDATA_CUSTOM_KEY
import io.tolgee.formats.android.AndroidStringValue
import io.tolgee.formats.android.PluralUnit
import io.tolgee.formats.android.StringArrayUnit
import io.tolgee.formats.android.StringUnit
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AndroidStringsXmlProcessor(override val context: FileProcessorContext) : ImportFileProcessor() {
  override fun process() {
    val parsed = parse()

    parsed.items.forEach { (keyName, item) ->
      when (item) {
        is StringUnit -> handleString(keyName, item)
        is PluralUnit -> handlePlural(keyName, item)
        is StringArrayUnit -> handleStringsArray(keyName, item)
        else -> {}
      }
    }
  }

  private fun parse() =
    try {
      AndroidStringsXmlParser(xmlEventReader).parse()
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
    }

  private fun handleString(
    keyName: String,
    it: StringUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    val text = it.value?.string
    context.addKeyDescription(keyName, it.comment)
    context.addTranslation(
      keyName,
      guessedLanguage,
      convertMessage(text),
      pluralArgName = null,
      rawData = text,
      convertedBy = ImportFormat.ANDROID_XML,
    )
    setCustomWrappedWithCdata(keyName, it.value)
  }

  private fun setCustomWrappedWithCdata(
    keyName: String,
    value: Collection<AndroidStringValue>,
  ) {
    val isWrappedCdata = value.any { it.isWrappedCdata }
    if (isWrappedCdata) {
      context.setCustom(keyName, ANDROID_CDATA_CUSTOM_KEY, true)
    }
  }

  private fun setCustomWrappedWithCdata(
    keyName: String,
    value: AndroidStringValue?,
  ) {
    setCustomWrappedWithCdata(keyName, value?.let { listOf(it) } ?: emptyList())
  }

  private fun handlePlural(
    keyName: String,
    it: PluralUnit,
  ) {
    if (keyName.isBlank()) {
      return
    }
    val rawData = it.items.map { it.key to it.value.string }.toMap()

    val converted =
      messageConvertor.convert(
        rawData = rawData,
        languageTag = guessedLanguage,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        context.projectIcuPlaceholdersEnabled,
      )

    context.addKeyDescription(keyName, it.comment)
    context.addTranslation(
      keyName,
      guessedLanguage,
      converted.message,
      pluralArgName = converted.pluralArgName,
      rawData = rawData,
      convertedBy = importFormat,
    )

    setCustomWrappedWithCdata(keyName, it.items.map { it.value })
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

      val text = item.value?.string
      context.addKeyDescription(keyName, item.comment)
      context.addTranslation(
        keyNameWithIndex,
        guessedLanguage,
        convertMessage(text),
        pluralArgName = null,
        rawData = text,
        convertedBy = importFormat,
      )
      setCustomWrappedWithCdata(keyNameWithIndex, item.value)
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

  private fun convertMessage(message: String?): String? {
    if (message == null) {
      return null
    }
    return messageConvertor.convert(
      message,
      guessedLanguage,
      context.importSettings.convertPlaceholdersToIcu,
      context.projectIcuPlaceholdersEnabled,
    ).message
  }

  companion object {
    val LANGUAGE_GUESS_REGEX = Regex("values-(?<language>[a-zA-Z]{2,3})(-r(?<region>[a-zA-Z]{2,3}))?")

    private val importFormat = ImportFormat.ANDROID_XML

    private val messageConvertor = importFormat.messageConvertor
  }
}
