package io.tolgee.fixtures

import org.springframework.mock.web.MockMultipartFile

data class TuData(
  val source: String,
  val targets: Map<String, String>,
)

data class TuRaw(
  val tuid: String?,
  val source: String,
  val targets: Map<String, String>,
)

fun tu(
  source: String,
  targets: Map<String, String>,
) = TuData(source, targets)

/** Builds TMX XML with auto-assigned sequential tuids starting at 1. */
fun buildTmx(
  srcLang: String,
  units: List<TuData>,
): String = buildTmxRaw(srcLang, units.mapIndexed { i, u -> TuRaw((i + 1).toString(), u.source, u.targets) })

/** Builds TMX XML with explicit (or null) tuids for each `<tu>`. */
fun buildTmxRaw(
  srcLang: String,
  units: List<TuRaw>,
): String {
  val sb = StringBuilder()
  sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
  sb.appendLine("""<tmx version="1.4">""")
  sb.appendLine("""  <header srclang="${escapeXml(srcLang)}" datatype="PlainText" creationtool="test"/>""")
  sb.appendLine("""  <body>""")
  units.forEach { unit ->
    val tuidAttr = if (unit.tuid != null) """ tuid="${escapeXml(unit.tuid)}"""" else ""
    sb.appendLine("""    <tu$tuidAttr>""")
    sb.appendLine(
      """      <tuv xml:lang="${escapeXml(srcLang)}"><seg>${escapeXml(unit.source)}</seg></tuv>""",
    )
    unit.targets.forEach { (lang, text) ->
      sb.appendLine("""      <tuv xml:lang="${escapeXml(lang)}"><seg>${escapeXml(text)}</seg></tuv>""")
    }
    sb.appendLine("""    </tu>""")
  }
  sb.appendLine("""  </body>""")
  sb.appendLine("""</tmx>""")
  return sb.toString()
}

private fun escapeXml(s: String): String =
  s
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

fun mockTmxFile(content: String): MockMultipartFile =
  MockMultipartFile("file", "test.tmx", "application/xml", content.toByteArray())
