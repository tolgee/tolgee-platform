package io.tolgee.ee.service.translationMemory.tmx

/**
 * One `<tu>` row to write. `translations` is unique by language tag — if multiple variants
 * for the same lang exist for one source, the producer splits them into separate units so
 * the exported XML never has duplicate `<tuv xml:lang>` siblings (invalid TMX).
 *
 * `tuid` carries the original value for stored entries that came from a TMX import; the
 * exporter fills in a sequential auto-tuid when it is null.
 */
data class TmxExportUnit(
  val tuid: String?,
  val sourceText: String,
  val translations: List<Pair<String, String>>,
)
