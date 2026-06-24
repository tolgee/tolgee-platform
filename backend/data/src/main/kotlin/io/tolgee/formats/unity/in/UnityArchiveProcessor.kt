package io.tolgee.formats.unity.`in`

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.unity.UnityIdentity

/**
 * Detects a Unity Localization collection inside an already-unpacked archive and merges its
 * cross-referenced `.asset` files (SharedTableData + per-locale StringTable) into one synthetic
 * `<collection>.unity` file per collection, leaving any non-Unity entries untouched.
 */
class UnityArchiveProcessor(
  private val objectMapper: ObjectMapper,
  yamlMapper: ObjectMapper,
) {
  private val parser = UnityAssetParser(yamlMapper)

  fun isUnity(files: Collection<ImportFileDto>): Boolean {
    val assets = files.filter { it.name.endsWith(".asset") }
    if (assets.isEmpty()) {
      return false
    }
    val hasShared = assets.any { parseSharedData(it) != null }
    val hasTable = assets.any { parseStringTable(it) != null }
    return hasShared && hasTable
  }

  fun merge(files: Collection<ImportFileDto>): List<ImportFileDto> {
    val sharedByGuid = mutableMapOf<String, SharedData>()
    val tables = mutableListOf<StringTable>()

    files.filter { it.name.endsWith(".asset") }.forEach { file ->
      parseSharedData(file)?.let { shared ->
        metaGuid(files, file.name)?.let { guid -> sharedByGuid[guid] = shared.copyWithGuid(guid) }
        return@forEach
      }
      parseStringTable(file)?.let { tables.add(it) }
    }

    val result = mutableListOf<ImportFileDto>()
    result.addAll(passthrough(files))
    sharedByGuid.forEach { (guid, shared) ->
      val collectionTables = tables.filter { it.sharedGuid == guid }
      result.add(buildSyntheticFile(shared, collectionTables))
    }
    return result
  }

  private fun passthrough(files: Collection<ImportFileDto>): List<ImportFileDto> {
    return files.filterNot { it.name.endsWith(".asset") || it.name.endsWith(".asset.meta") }
  }

  private fun buildSyntheticFile(
    shared: SharedData,
    tables: List<StringTable>,
  ): ImportFileDto {
    val keys =
      shared.entries.map { (id, name) ->
        val smart = tables.any { it.entries[id]?.isSmart == true }
        val translations =
          tables.mapNotNull { table ->
            val entry = table.entries[id] ?: return@mapNotNull null
            localeEntry(table.locale, entry)
          }
        UnityKeyImportModel(name, id, shared.guid, smart, translations)
      }
    val model = UnityCollectionImportModel(shared.collectionName, keys)
    return ImportFileDto("${shared.collectionName}.unity", objectMapper.writeValueAsBytes(model))
  }

  private fun localeEntry(
    locale: String,
    entry: TableEntry,
  ): UnityLocaleImportEntry {
    val forms = pluralForms(entry, locale)
    if (forms != null) {
      return UnityLocaleImportEntry(locale = locale, pluralForms = forms)
    }
    return UnityLocaleImportEntry(locale = locale, value = entry.value)
  }

  private fun pluralForms(
    entry: TableEntry,
    locale: String,
  ): Map<String, String>? {
    if (!entry.isSmart) {
      return null
    }
    val match = PLURAL_PATTERN.matchEntire(entry.value) ?: return null
    val options = splitOptions(match.groupValues[1])
    val order = UnityIdentity.pluralOrder(locale)
    if (options.isEmpty()) {
      return null
    }
    return order.mapIndexed { index, category -> category to (options.getOrNull(index) ?: options.last()) }.toMap()
  }

  private fun splitOptions(options: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var escaped = false
    options.forEach { char ->
      if (escaped) {
        current.append(char)
        escaped = false
        return@forEach
      }
      if (char == '\\') {
        current.append(char)
        escaped = true
        return@forEach
      }
      if (char == '|') {
        result.add(current.toString())
        current.clear()
        return@forEach
      }
      current.append(char)
    }
    result.add(current.toString())
    return result
  }

  private fun parseSharedData(file: ImportFileDto): SharedData? {
    val mb = parser.parseMonoBehaviour(file.data) ?: return null
    val entriesNode = mb.get("m_Entries") ?: return null
    val entries =
      entriesNode
        .mapNotNull { node ->
          val id = node.get("m_Id")?.asLong() ?: return@mapNotNull null
          val key = node.get("m_Key")?.asText() ?: return@mapNotNull null
          id to key
        }.toMap()
    if (entries.isEmpty()) {
      return null
    }
    val name = mb.get("m_TableCollectionName")?.asText() ?: return null
    return SharedData(name, "", entries)
  }

  private fun parseStringTable(file: ImportFileDto): StringTable? {
    val mb = parser.parseMonoBehaviour(file.data) ?: return null
    val entriesNode = mb.get("m_TableEntries") ?: return null
    val sharedGuid = mb.get("m_SharedData")?.get("guid")?.asText() ?: return null
    val locale = mb.get("m_LocaleId")?.get("m_Code")?.asText() ?: return null
    val entries =
      entriesNode
        .mapNotNull { node ->
          val id = node.get("m_Id")?.asLong() ?: return@mapNotNull null
          val localized = node.get("m_Localized")?.asText() ?: ""
          val isSmart = node.get("m_IsSmart")?.asInt() == 1
          id to TableEntry(localized, isSmart)
        }.toMap()
    return StringTable(sharedGuid, locale, entries)
  }

  private fun metaGuid(
    files: Collection<ImportFileDto>,
    assetName: String,
  ): String? {
    val meta = files.firstOrNull { it.name == "$assetName.meta" } ?: return null
    return parser.parseMetaGuid(meta.data)
  }

  private fun SharedData.copyWithGuid(guid: String) = SharedData(collectionName, guid, entries)

  private class SharedData(
    val collectionName: String,
    val guid: String,
    val entries: Map<Long, String>,
  )

  private class StringTable(
    val sharedGuid: String,
    val locale: String,
    val entries: Map<Long, TableEntry>,
  )

  private class TableEntry(
    val value: String,
    val isSmart: Boolean,
  )

  companion object {
    private val PLURAL_PATTERN = Regex("""\{[^:{}]*:plural:(.*)}""", RegexOption.DOT_MATCHES_ALL)
  }
}
