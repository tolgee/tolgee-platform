package io.tolgee.formats.unity.out

class UnityCollectionExportModel(
  val collectionName: String,
  val sharedTableDataGuid: String,
) {
  /** key name -> key (ordered by Key Id for deterministic output) */
  val keys = sortedMapOf<Long, UnityKeyExportModel>()

  /** locale tag -> (Key Id -> localized entry) */
  val locales = sortedMapOf<String, MutableMap<Long, UnityLocalizedEntry>>()

  fun localeEntries(localeTag: String): MutableMap<Long, UnityLocalizedEntry> {
    return locales.getOrPut(localeTag) { sortedMapOf() }
  }
}

class UnityKeyExportModel(
  val id: Long,
  val name: String,
  val comment: String?,
)

class UnityLocalizedEntry(
  val value: String,
  val isSmart: Boolean,
)
