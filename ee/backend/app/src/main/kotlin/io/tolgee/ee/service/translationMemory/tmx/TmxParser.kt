package io.tolgee.ee.service.translationMemory.tmx

import io.tolgee.util.XmlSecurity
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream

/**
 * Parses a TMX file into entries ready to be stored in a TM.
 *
 * The TM's declared [tmSourceLanguageTag] is **authoritative**: every parsed entry uses it as
 * the source. The TMX's own `<header srclang>` attribute is intentionally ignored — TMX files
 * commonly declare a different source language than the TM they're being imported into (e.g.
 * a publicly-distributed en→cs/sk TMX imported into a cs-based TM), and respecting the TMX
 * value would land entries whose `source_text` is in the wrong language, never matching the
 * project's base translations downstream.
 *
 * `<tu>` elements that don't carry a `<tuv xml:lang="…">` matching [tmSourceLanguageTag] are
 * silently dropped — there's nothing meaningful to use as a source.
 */
class TmxParser(
  private val tmSourceLanguageTag: String,
) {
  fun parse(inputStream: InputStream): List<TmxParsedEntry> {
    // Centralised hardening (FEATURE_SECURE_PROCESSING, disallow-doctype-decl, disabled XInclude
    // and entity expansion) shared with the rest of the XML parsers in the codebase.
    val factory = XmlSecurity.newSecureDocumentBuilderFactory()
    factory.isNamespaceAware = true
    val doc = factory.newDocumentBuilder().parse(inputStream)

    val tuElements = doc.getElementsByTagName("tu")
    val result = mutableListOf<TmxParsedEntry>()

    for (i in 0 until tuElements.length) {
      val tu = tuElements.item(i) as Element
      val tuid = tu.getAttribute("tuid").ifBlank { null }
      result.addAll(parseTu(tu, tuid))
    }

    return result
  }

  private fun parseTu(
    tu: Element,
    tuid: String?,
  ): List<TmxParsedEntry> {
    val tuvElements = tu.getElementsByTagName("tuv")
    val tuvList = toElementList(tuvElements)

    val sourceTuv =
      tuvList.firstOrNull { getLang(it).equals(tmSourceLanguageTag, ignoreCase = true) }
        ?: return emptyList()
    val sourceText = getSegText(sourceTuv).trim()
    if (sourceText.isBlank()) return emptyList()

    return tuvList
      .filter { !getLang(it).equals(tmSourceLanguageTag, ignoreCase = true) }
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
