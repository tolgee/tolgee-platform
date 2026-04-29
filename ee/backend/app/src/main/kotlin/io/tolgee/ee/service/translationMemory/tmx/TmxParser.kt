package io.tolgee.ee.service.translationMemory.tmx

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

class TmxParser(
  private val tmSourceLanguageTag: String,
) {
  fun parse(inputStream: InputStream): List<TmxParsedEntry> {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    // Disable DTD loading — TMX files may reference tmx14.dtd which is not available
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    val doc = factory.newDocumentBuilder().parse(inputStream)

    val headerElements = doc.getElementsByTagName("header")
    val srcLang =
      if (headerElements.length > 0) {
        (headerElements.item(0) as Element).getAttribute("srclang").ifBlank { tmSourceLanguageTag }
      } else {
        tmSourceLanguageTag
      }

    val tuElements = doc.getElementsByTagName("tu")
    val result = mutableListOf<TmxParsedEntry>()

    for (i in 0 until tuElements.length) {
      val tu = tuElements.item(i) as Element
      val tuid = tu.getAttribute("tuid").ifBlank { null }
      result.addAll(parseTu(tu, srcLang, tuid))
    }

    return result
  }

  private fun parseTu(
    tu: Element,
    srcLang: String,
    tuid: String?,
  ): List<TmxParsedEntry> {
    val tuvElements = tu.getElementsByTagName("tuv")
    val tuvList = toElementList(tuvElements)

    val sourceTuv = tuvList.firstOrNull { getLang(it).equals(srcLang, ignoreCase = true) } ?: return emptyList()
    val sourceText = getSegText(sourceTuv).trim()
    if (sourceText.isBlank()) return emptyList()

    return tuvList
      .filter { !getLang(it).equals(srcLang, ignoreCase = true) }
      .filter { getSegText(it).trim().isNotBlank() }
      .map { tuv ->
        TmxParsedEntry(
          sourceText = sourceText,
          targetText = getSegText(tuv).trim(),
          targetLanguageTag = getLang(tuv),
          tuid = tuid,
        )
      }
  }

  private fun getLang(tuv: Element): String {
    // Try xml:lang first (namespace-aware), then fall back to plain attribute
    val lang = tuv.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang")
    if (lang.isNotBlank()) return lang
    return tuv.getAttribute("xml:lang").ifBlank { tuv.getAttribute("lang") }
  }

  private fun getSegText(tuv: Element): String {
    val segs = tuv.getElementsByTagName("seg")
    if (segs.length == 0) return ""
    return segs.item(0).textContent ?: ""
  }

  private fun toElementList(nodeList: NodeList): List<Element> {
    return (0 until nodeList.length).map { nodeList.item(it) as Element }
  }
}
