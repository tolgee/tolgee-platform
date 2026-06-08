package io.tolgee.formats.unity.out

import io.tolgee.formats.unity.UnityFormatConstants
import io.tolgee.formats.unity.UnityIdentity
import java.io.InputStream

/**
 * Serializes a Unity Localization collection to its on-disk `.asset` (+ `.meta`) representation.
 * The field layout and the [UnityFormatConstants] script GUIDs are tied to the pinned
 * com.unity.localization version and must match it.
 */
class UnityAssetWriter(
  private val folder: String,
  private val model: UnityCollectionExportModel,
) {
  private val sharedGuid = model.sharedTableDataGuid
  private val collectionGuid = UnityIdentity.deriveGuid("$sharedGuid:collection")

  fun produceFiles(): Map<String, InputStream> {
    val files = LinkedHashMap<String, InputStream>()
    val sharedName = "${model.collectionName} Shared Data"
    putAsset(files, sharedName, sharedTableData())
    putMeta(files, sharedName, sharedGuid)

    model.locales.forEach { (locale, _) ->
      val tableName = "${model.collectionName}_$locale"
      putAsset(files, tableName, stringTable(locale))
      putMeta(files, tableName, stringTableGuid(locale))
    }

    putAsset(files, model.collectionName, stringTableCollection())
    putMeta(files, model.collectionName, collectionGuid)
    return files
  }

  private fun stringTableGuid(locale: String) = UnityIdentity.deriveGuid("$sharedGuid:table:$locale")

  private fun sharedTableData(): String {
    val entries =
      model.keys.values.joinToString("\n") { key ->
        "  - m_Id: ${key.id}\n" +
          "    m_Key: ${yamlString(key.name)}\n" +
          "    m_Metadata:\n" +
          "      m_Items: []"
      }
    val maxId = model.keys.keys.maxOrNull() ?: 0L
    val trackedIds = model.keys.keys.joinToString("\n") { "    - $it" }.ifEmpty { "    []" }
    return assetDocument(UnityFormatConstants.SHARED_TABLE_DATA_SCRIPT_GUID) {
      append("  m_Name: ${yamlString("${model.collectionName} Shared Data")}\n")
      append("  m_TableCollectionName: ${yamlString(model.collectionName)}\n")
      append("  m_Entries:\n")
      append(entries.ifEmpty { "  []" })
      append("\n")
      append("  m_KeyGenerator:\n")
      append("    m_NextAvailableId: ${maxId + 1}\n")
      if (model.keys.isEmpty()) {
        append("    m_TrackedIds: []\n")
        return@assetDocument
      }
      append("    m_TrackedIds:\n")
      append(trackedIds)
      append("\n")
    }
  }

  private fun stringTable(locale: String): String {
    val entries = model.localeEntries(locale)
    val rendered =
      model.keys.values.joinToString("\n") { key ->
        val value = entries[key.id]?.value ?: ""
        "  - m_Id: ${key.id}\n" +
          "    m_Localized: ${yamlString(value)}\n" +
          "    m_Metadata:\n" +
          "      m_Items: []\n" +
          "    m_IsSmart: ${smartFlag(key.isSmart)}"
      }
    return assetDocument(UnityFormatConstants.STRING_TABLE_SCRIPT_GUID) {
      append("  m_Name: ${yamlString("${model.collectionName}_$locale")}\n")
      append("  m_SharedData: {fileID: 11400000, guid: $sharedGuid, type: 2}\n")
      append("  m_TableCollectionName: ${yamlString(model.collectionName)}\n")
      append("  m_LocaleId:\n")
      append("    m_Code: ${yamlString(locale)}\n")
      append("  m_TableEntries:\n")
      append(rendered.ifEmpty { "  []" })
      append("\n")
    }
  }

  private fun stringTableCollection(): String {
    val tables =
      model.locales.keys.joinToString("\n") { locale ->
        "  - m_FileID: 11400000\n    m_GUID: ${stringTableGuid(locale)}"
      }
    return assetDocument(UnityFormatConstants.STRING_TABLE_COLLECTION_SCRIPT_GUID) {
      append("  m_Name: ${yamlString(model.collectionName)}\n")
      append("  m_SharedTableData: {fileID: 11400000, guid: $sharedGuid, type: 2}\n")
      append("  m_Tables:\n")
      append(tables.ifEmpty { "  []" })
      append("\n")
    }
  }

  private fun assetDocument(
    scriptGuid: String,
    body: StringBuilder.() -> Unit,
  ): String {
    val sb = StringBuilder()
    sb.append("%YAML 1.1\n")
    sb.append("%TAG !u! tag:unity3d.com,2011:\n")
    sb.append("--- !u!114 &11400000\n")
    sb.append("MonoBehaviour:\n")
    sb.append("  m_ObjectHideFlags: 0\n")
    sb.append("  m_CorrespondingSourceObject: {fileID: 0}\n")
    sb.append("  m_PrefabInstance: {fileID: 0}\n")
    sb.append("  m_PrefabAsset: {fileID: 0}\n")
    sb.append("  m_GameObject: {fileID: 0}\n")
    sb.append("  m_Enabled: 1\n")
    sb.append("  m_EditorHideFlags: 0\n")
    sb.append("  m_Script: {fileID: 11500000, guid: $scriptGuid, type: 3}\n")
    sb.body()
    return sb.toString()
  }

  private fun putAsset(
    files: MutableMap<String, InputStream>,
    name: String,
    content: String,
  ) {
    files[path("$name.asset")] = content.byteInputStream()
  }

  private fun putMeta(
    files: MutableMap<String, InputStream>,
    name: String,
    guid: String,
  ) {
    files[path("$name.asset.meta")] = metaContent(guid).byteInputStream()
  }

  private fun path(fileName: String): String {
    if (folder.isEmpty()) {
      return fileName
    }
    return "$folder/$fileName"
  }

  private fun metaContent(guid: String): String {
    return "fileFormatVersion: 2\n" +
      "guid: $guid\n" +
      "NativeFormatImporter:\n" +
      "  externalObjects: {}\n" +
      "  mainObjectFileID: 11400000\n" +
      "  userData: \n" +
      "  assetBundleName: \n" +
      "  assetBundleVariant: \n"
  }

  private fun smartFlag(smart: Boolean): Int {
    if (smart) return 1
    return 0
  }

  private fun yamlString(value: String): String {
    val escaped =
      value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    return "\"$escaped\""
  }
}
