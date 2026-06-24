package io.tolgee.formats.unity.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.unity.UnityFormatConstants
import io.tolgee.formats.unity.out.UnityExporter
import io.tolgee.service.dataImport.processors.FileProcessorContext

class UnityProcessor(
  override val context: FileProcessorContext,
  private val objectMapper: ObjectMapper,
) : ImportFileProcessor() {
  override fun process() {
    val model =
      try {
        objectMapper.readValue(context.file.data, UnityCollectionImportModel::class.java)
      } catch (e: Exception) {
        throw ImportCannotParseFileException(context.file.name, e.message)
      }

    context.namespace = model.collectionName.takeIf { it != UnityExporter.DEFAULT_COLLECTION_NAME }

    model.keys.forEach { key ->
      key.translations.forEach { entry -> addTranslation(key, entry) }
      context.setCustom(key.name, UnityFormatConstants.CUSTOM_KEY_ID, key.keyId)
      context.setCustom(key.name, UnityFormatConstants.CUSTOM_SHARED_TABLE_DATA_GUID, key.sharedTableDataGuid)
      context.setCustom(key.name, UnityFormatConstants.CUSTOM_IS_SMART, key.isSmart)
    }
  }

  private fun addTranslation(
    key: UnityKeyImportModel,
    entry: UnityLocaleImportEntry,
  ) {
    if (entry.pluralForms != null) {
      addConverted(key.name, entry.locale, entry.pluralForms)
      return
    }
    if (key.isSmart) {
      addConverted(key.name, entry.locale, entry.value ?: "")
      return
    }
    context.addTranslation(key.name, entry.locale, ForceIcuEscaper(entry.value ?: "").escaped)
  }

  private fun addConverted(
    keyName: String,
    locale: String,
    rawData: Any,
  ) {
    val converted =
      messageConvertor.convert(
        rawData = rawData,
        languageTag = locale,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )
    context.addTranslation(
      keyName = keyName,
      languageName = locale,
      value = converted.message,
      convertedBy = importFormat,
      rawData = rawData,
      pluralArgName = converted.pluralArgName,
    )
  }

  companion object {
    private val importFormat = ImportFormat.UNITY
    private val messageConvertor = importFormat.messageConvertor
  }
}
