package io.tolgee.ee.service.translationMemory.tmx

import io.tolgee.util.XmlSecurity
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream

class TmxParser(
  private val tmSourceLanguageTag: String,
) {
  fun parse(inputStream: InputStream): List<TmxParsedEntry> {
    // Centralised hardening (FEATURE_SECURE_PROCESSING, disallow-doctype-decl, disabled XInclude
    // and entity expansion) shared with the rest of the XML parsers in the codebase.
    val factory = XmlSecurity.newSecureDocumentBuilderFactory()
    factory.isNamespaceAware = true
    val doc = factory.newDocumentBuilder().parse(inputStream)

    val headerElements = doc.getElementsByTagName("header")
    val srcLang = resolveSourceLanguage(headerElements)

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

  /**
   * Resolves the source language from the `<header srclang=…>` attribute, falling back to the
   * TM's declared source when the attribute is missing, blank, or the TMX wildcard `*` (the
   * spec value meaning "no single source language"). Without the wildcard branch every `<tu>`
   * would be dropped because no `<tuv xml:lang="*">` ever matches a real translation row.
   */
  private fun resolveSourceLanguage(headerElements: NodeList): String {
    if (headerElements.length == 0) return tmSourceLanguageTag
    val declared = (headerElements.item(0) as Element).getAttribute("srclang")
    if (declared.isBlank() || declared == "*") return tmSourceLanguageTag
    return declared
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
