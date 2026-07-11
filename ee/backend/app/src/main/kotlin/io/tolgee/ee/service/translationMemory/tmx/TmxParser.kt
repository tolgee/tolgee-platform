package io.tolgee.ee.service.translationMemory.tmx

import io.tolgee.model.translationMemory.TranslationMemoryEntry
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
 * silently dropped — there's nothing meaningful to use as a source. Segments exceeding
 * [TranslationMemoryEntry.MAX_TEXT_LENGTH] are dropped too, but the count of dropped entries
 * is reported on [TmxParseResult.skippedOversize] so the import response can surface them
 * (rather than letting long segments disappear without a trace).
 */
class TmxParser(
  private val tmSourceLanguageTag: String,
) {
  fun parse(inputStream: InputStream): TmxParseResult {
    // Centralised hardening (FEATURE_SECURE_PROCESSING, disallow-doctype-decl, disabled XInclude
    // and entity expansion) shared with the rest of the XML parsers in the codebase.
    val factory = XmlSecurity.newSecureDocumentBuilderFactory()
    factory.isNamespaceAware = true
    val doc = factory.newDocumentBuilder().parse(inputStream)

    val tuElements = doc.getElementsByTagName("tu")
    val entries = mutableListOf<TmxParsedEntry>()
    var skippedOversize = 0

    for (i in 0 until tuElements.length) {
      val tu = tuElements.item(i) as Element
      val tuid = tu.getAttribute("tuid").ifBlank { null }
      val tuResult = parseTu(tu, tuid)
      entries.addAll(tuResult.entries)
      skippedOversize += tuResult.skippedOversize
    }

    return TmxParseResult(entries = entries, skippedOversize = skippedOversize)
  }

  private fun parseTu(
    tu: Element,
    tuid: String?,
  ): ParseTuResult {
    val tuvList = toElementList(tu.getElementsByTagName("tuv"))

    val sourceTuv =
      tuvList.firstOrNull { getLang(it).equals(tmSourceLanguageTag, ignoreCase = true) }
        ?: return ParseTuResult.EMPTY
    val sourceText = getSegText(sourceTuv).trim()
    if (sourceText.isBlank()) return ParseTuResult.EMPTY

    if (sourceText.length > TranslationMemoryEntry.MAX_TEXT_LENGTH) {
      // Oversize source kills the whole tu — count every non-source tuv with non-blank content
      // as a would-have-been entry so the import response can report the loss.
      val wouldHaveBeen =
        tuvList.count {
          !getLang(it).equals(tmSourceLanguageTag, ignoreCase = true) &&
            getSegText(it).trim().isNotBlank()
        }
      return ParseTuResult(entries = emptyList(), skippedOversize = wouldHaveBeen)
    }

    val entries = mutableListOf<TmxParsedEntry>()
    var skippedOversize = 0
    for (tuv in tuvList) {
      if (getLang(tuv).equals(tmSourceLanguageTag, ignoreCase = true)) continue
      val targetText = getSegText(tuv).trim()
      if (targetText.isBlank()) continue
      if (targetText.length > TranslationMemoryEntry.MAX_TEXT_LENGTH) {
        skippedOversize++
        continue
      }
      entries.add(
        TmxParsedEntry(
          sourceText = sourceText,
          targetText = targetText,
          targetLanguageTag = getLang(tuv),
          tuid = tuid,
        ),
      )
    }
    return ParseTuResult(entries = entries, skippedOversize = skippedOversize)
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

  private data class ParseTuResult(
    val entries: List<TmxParsedEntry>,
    val skippedOversize: Int,
  ) {
    companion object {
      val EMPTY = ParseTuResult(entries = emptyList(), skippedOversize = 0)
    }
  }
}
