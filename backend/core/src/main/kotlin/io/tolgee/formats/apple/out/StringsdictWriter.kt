package io.tolgee.formats.apple.out

import io.tolgee.formats.MobileStringEscaper
import io.tolgee.formats.paramConvertors.`in`.AppleToIcuPlaceholderConvertor.Companion.APPLE_PLACEHOLDER_REGEX
import io.tolgee.util.replaceEntire
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream

class StringsdictWriter {
  private val document: Document = DocumentHelper.createDocument()
  private val root = DocumentHelper.createElement("plist")

  init {
    document.add(root)
    document.addDocType("plist", "-//Apple//DTD PLIST 1.0//EN", "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
    root.addAttribute("version", "1.0")
    root.add(DocumentHelper.createElement("dict"))
  }

  fun addEntry(
    key: String,
    pluralForms: Map<String, String>,
  ) {
    val dictElement = root.element("dict")

    val keyElement = DocumentHelper.createElement("key")
    keyElement.text = key
    dictElement.add(keyElement)

    val dictValueElement = DocumentHelper.createElement("dict")
    dictElement.add(dictValueElement)

    // add other necessary elements as needed
    val keyLocalizedFormatElement = DocumentHelper.createElement("key")
    keyLocalizedFormatElement.text = "NSStringLocalizedFormatKey"
    dictValueElement.add(keyLocalizedFormatElement)

    val stringLocalizedFormatElement = DocumentHelper.createElement("string")
    stringLocalizedFormatElement.text = "%#@format@"
    dictValueElement.add(stringLocalizedFormatElement)

    val keyFormatElement = DocumentHelper.createElement("key")
    keyFormatElement.text = "format"
    dictValueElement.add(keyFormatElement)

    val dictFormatElement = DocumentHelper.createElement("dict")
    dictValueElement.add(dictFormatElement)

    addFormatSpec(dictFormatElement)
    addFormatValueType(dictFormatElement, pluralForms.values)

    addPluralForms(pluralForms, dictFormatElement)
  }

  private fun addPluralForms(
    pluralForms: Map<String, String>,
    dictFormatElement: Element,
  ) {
    pluralForms.forEach { (formKey, translation) ->
      val keyQuantityElement = DocumentHelper.createElement("key")
      keyQuantityElement.text = formKey
      dictFormatElement.add(keyQuantityElement)

      val stringQuantityElement = DocumentHelper.createElement("string")
      stringQuantityElement.text = translation.escaped()
      dictFormatElement.add(stringQuantityElement)
    }
  }

  private fun addFormatSpec(dictFormatElement: Element) {
    val formatSpecKeyElement = DocumentHelper.createElement("key")
    formatSpecKeyElement.text = "NSStringFormatSpecTypeKey"
    dictFormatElement.add(formatSpecKeyElement)

    val formatSpecStringElement = DocumentHelper.createElement("string")
    formatSpecStringElement.text = "NSStringPluralRuleType"
    dictFormatElement.add(formatSpecStringElement)
  }

  private fun addFormatValueType(
    dictFormatElement: Element,
    pluralFormsValues: Collection<String>,
  ) {
    val formatSpecKeyElement = DocumentHelper.createElement("key")
    formatSpecKeyElement.text = "NSStringFormatValueTypeKey"
    dictFormatElement.add(formatSpecKeyElement)

    val formatSpecStringElement = DocumentHelper.createElement("string")
    formatSpecStringElement.text = getFormatSpecString(pluralFormsValues)
    dictFormatElement.add(formatSpecStringElement)
  }

  private fun getFormatSpecString(pluralFormsValues: Collection<String>): String {
    return getFormatSpecSignFromStrings(pluralFormsValues) ?: "lld"
  }

  private fun getFormatSpecSignFromStrings(strings: Collection<String>): String? {
    val mostCommon = mostCommonMatch(strings).mapNotNull { it.onlyPlaceholderType }
    val filtered = mostCommon.filter { it != "@" }.toList()
    filtered.firstOrNull { it == "d" || it == "f" || it == "lld" }?.let { return it }
    return filtered.firstOrNull()
  }

  private val String.onlyPlaceholderType: String?
    get() = APPLE_PLACEHOLDER_REGEX.replaceEntire(this, "${'$'}{length}${'$'}{specifier}")

  private fun mostCommonMatch(strings: Collection<String>): Sequence<String> {
    val matches = LinkedHashMap<String, Int>()
    for (str in strings) {
      val matchResult = APPLE_PLACEHOLDER_REGEX.findAll(str)
      matchResult.forEach { match ->
        val matchText = match.value
        matches[matchText] = matches.getOrDefault(matchText, 0) + 1
      }
    }
    return matches
      .asSequence()
      .sortedByDescending { it.value }
      .map { it.key }
  }

  private fun String.escaped(): String {
    return MobileStringEscaper(
      string = this,
      escapeApos = false,
      keepPercentSignEscaped = true,
      quoteMoreWhitespaces = false,
      escapeNewLines = false,
      escapeQuotes = false,
      utfSymbolCharacter = 'U',
    ).escape()
  }

  val result: InputStream
    get() {
      val format = OutputFormat.createPrettyPrint()
      val outputStream = ByteArrayOutputStream()
      val writer = XMLWriter(outputStream, format)
      writer.write(document)
      return outputStream.toByteArray().inputStream()
    }
}
