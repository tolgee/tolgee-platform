package io.tolgee.formats.unity.out

class UnityCollectionExportModel(
  val collectionName: String,
  val sharedTableDataGuid: String,
) {
  val keys = sortedMapOf<Long, UnityKeyExportModel>()

  val locales = sortedMapOf<String, MutableMap<Long, UnityLocalizedEntry>>()

  fun localeEntries(localeTag: String): MutableMap<Long, UnityLocalizedEntry> {
    return locales.getOrPut(localeTag) { sortedMapOf() }
  }
}

class UnityKeyExportModel(
  val id: Long,
  val name: String,
  val isSmart: Boolean,
)

class UnityLocalizedEntry(
  val value: String,
)
