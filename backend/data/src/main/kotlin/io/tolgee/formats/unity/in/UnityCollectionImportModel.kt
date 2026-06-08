package io.tolgee.formats.unity.`in`

/**
 * Internal normalized representation a [UnityArchiveProcessor] builds from a Unity Localization
 * collection (SharedTableData + per-locale StringTables) and serializes into a single synthetic
 * import file for [UnityProcessor] to consume.
 */
data class UnityCollectionImportModel(
  val collectionName: String = "",
  val keys: List<UnityKeyImportModel> = emptyList(),
)

data class UnityKeyImportModel(
  val name: String = "",
  val keyId: Long = 0,
  val sharedTableDataGuid: String = "",
  val isSmart: Boolean = false,
  val translations: List<UnityLocaleImportEntry> = emptyList(),
)

data class UnityLocaleImportEntry(
  val locale: String = "",
  val value: String? = null,
  val pluralForms: Map<String, String>? = null,
)
