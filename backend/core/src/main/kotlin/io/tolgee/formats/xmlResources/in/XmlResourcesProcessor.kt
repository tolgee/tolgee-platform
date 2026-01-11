package io.tolgee.formats.xmlResources.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.android.`in`.AndroidStringUnescaper
import io.tolgee.formats.compose.`in`.ComposeStringUnescaper
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.xmlResources.PluralUnit
import io.tolgee.formats.xmlResources.StringArrayUnit
import io.tolgee.formats.xmlResources.StringUnit
import io.tolgee.formats.xmlResources.XML_RESOURCES_CDATA_CUSTOM_KEY
import io.tolgee.formats.xmlResources.XmlResourcesParsingConstants
import io.tolgee.formats.xmlResources.XmlResourcesStringValue
import io.tolgee.service.dataImport.processors.FileProcessorContext
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class XmlResourcesProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
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
      XmlResourcesParser(
        xmlEventReader,
        stringUnescaper,
        supportedTags,
      ).parse()
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
      convertText(text),
      pluralArgName = null,
      rawData = text,
      convertedBy = importFormat,
    )
    setCustomWrappedWithCdata(keyName, it.value)
  }

  private fun setCustomWrappedWithCdata(
    keyName: String,
    value: Collection<XmlResourcesStringValue>,
  ) {
    val isWrappedCdata = value.any { it.isWrappedCdata }
    if (isWrappedCdata) {
      context.setCustom(keyName, XML_RESOURCES_CDATA_CUSTOM_KEY, true)
    }
  }

  private fun setCustomWrappedWithCdata(
    keyName: String,
    value: XmlResourcesStringValue?,
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

    val converted = convertMessage(rawData)

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
        convertText(text),
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

  private fun convertMessage(message: Any): MessageConvertorResult {
    return messageConvertor.convert(
      message,
      guessedLanguage,
      context.importSettings.convertPlaceholdersToIcu,
      context.projectIcuPlaceholdersEnabled,
    )
  }

  private fun convertText(text: String?): String? {
    return text?.let { convertMessage(it).message }
  }

  private val importFormat = context.mapping?.format ?: ImportFormat.ANDROID_XML

  private val messageConvertor = importFormat.messageConvertor

  private val stringUnescaper =
    if (importFormat == ImportFormat.COMPOSE_XML) {
      ComposeStringUnescaper.defaultFactory
    } else {
      AndroidStringUnescaper.defaultFactory
    }

  private val supportedTags =
    if (importFormat == ImportFormat.COMPOSE_XML) {
      emptySet()
    } else {
      XmlResourcesParsingConstants.androidSupportedTags
    }

  companion object {
    val LANGUAGE_GUESS_REGEX = Regex("values-(?<language>[a-zA-Z]{2,3})(-r(?<region>[a-zA-Z]{2,3}))?")
  }
}
