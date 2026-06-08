package io.tolgee.formats.unity.out

import io.tolgee.constants.Message
import io.tolgee.dtos.IExportParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.DEFAULT_PLURAL_ARGUMENT_NAME
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.escaping.IcuUnescaper
import io.tolgee.formats.populateForms
import io.tolgee.formats.unity.UnityFormatConstants
import io.tolgee.formats.unity.UnityIdentity
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class UnityExporter(
  private val translations: List<ExportTranslationView>,
  private val exportParams: IExportParams,
  private val isProjectIcuPlaceholdersEnabled: Boolean,
  private val filePathProvider: ExportFilePathProvider,
) : FileExporter {
  override fun produceFiles(): Map<String, InputStream> {
    val files = LinkedHashMap<String, InputStream>()
    translations
      .groupBy { it.key.namespace }
      .forEach { (namespace, namespaceTranslations) ->
        val model = buildModel(namespace, namespaceTranslations)
        val folder = filePathProvider.getFilePath(namespace = namespace, replaceExtension = false)
        files.putAll(UnityAssetWriter(folder, model).produceFiles())
      }
    return files
  }

  private fun buildModel(
    namespace: String?,
    namespaceTranslations: List<ExportTranslationView>,
  ): UnityCollectionExportModel {
    val collectionName = namespace?.takeIf { it.isNotBlank() } ?: DEFAULT_COLLECTION_NAME
    val sharedGuid = resolveSharedGuid(namespace, namespaceTranslations)
    val model = UnityCollectionExportModel(collectionName, sharedGuid)

    namespaceTranslations.groupBy { it.key.name }.forEach { (keyName, views) ->
      val keyId = resolveKeyId(namespace, keyName, views)
      val existing = model.keys[keyId]
      if (existing != null && existing.name != keyName) {
        throw BadRequestException(Message.UNITY_DUPLICATE_KEY_ID, listOf(keyId.toString(), existing.name, keyName))
      }

      val converted = views.associateWith { convert(it) }
      val smart = resolveEffectiveSmart(views, converted)
      model.keys[keyId] = UnityKeyExportModel(keyId, keyName, smart)

      views.forEach { view ->
        view.text ?: return@forEach
        model.localeEntries(view.languageTag)[keyId] = UnityLocalizedEntry(renderValue(view, converted[view], smart))
      }
    }
    return model
  }

  private fun resolveSharedGuid(
    namespace: String?,
    views: List<ExportTranslationView>,
  ): String {
    val preserved = views.mapNotNull { preservedString(it, UnityFormatConstants.CUSTOM_SHARED_TABLE_DATA_GUID) }.toSet()
    if (preserved.size > 1) {
      throw BadRequestException(Message.UNITY_SHARED_TABLE_DATA_GUID_CONFLICT, preserved.toList())
    }
    return preserved.firstOrNull() ?: UnityIdentity.deriveGuid("namespace:${namespace ?: ""}")
  }

  private fun resolveKeyId(
    namespace: String?,
    keyName: String,
    views: List<ExportTranslationView>,
  ): Long {
    val preserved = views.firstNotNullOfOrNull { preservedNumber(it, UnityFormatConstants.CUSTOM_KEY_ID) }
    return preserved?.toLong() ?: UnityIdentity.deriveKeyId(namespace, keyName)
  }

  private fun resolveEffectiveSmart(
    views: List<ExportTranslationView>,
    converted: Map<ExportTranslationView, PossiblePluralConversionResult?>,
  ): Boolean {
    val preserved = views.firstNotNullOfOrNull { preservedBoolean(it, UnityFormatConstants.CUSTOM_IS_SMART) }
    if (preserved != null) {
      return preserved
    }
    return views.any { isSmart(converted[it]) }
  }

  private fun isSmart(converted: PossiblePluralConversionResult?): Boolean {
    converted ?: return false
    return converted.isPlural() || converted.firstArgName != null
  }

  private fun convert(view: ExportTranslationView): PossiblePluralConversionResult? {
    val text = view.text ?: return null
    return IcuToUnityMessageConvertor(text, view.key.isPlural, isProjectIcuPlaceholdersEnabled).convert()
  }

  private fun renderValue(
    view: ExportTranslationView,
    converted: PossiblePluralConversionResult?,
    smart: Boolean,
  ): String {
    val text = view.text ?: return ""
    if (!smart || converted == null) {
      return IcuUnescaper(text).unescaped
    }
    if (!converted.isPlural()) {
      return converted.singleResult ?: ""
    }
    return buildPluralString(converted.formsResult!!, converted.argName, view.languageTag)
  }

  private fun buildPluralString(
    forms: Map<String, String>,
    argName: String?,
    languageTag: String,
  ): String {
    val populated = populateForms(languageTag, forms)
    val order = UnityIdentity.pluralOrder(languageTag)
    val otherValue = populated["other"] ?: ""
    val pipes = order.joinToString("|") { escapePipe(populated[it] ?: otherValue) }
    return "{${argName ?: DEFAULT_PLURAL_ARGUMENT_NAME}:plural:$pipes}"
  }

  private fun escapePipe(form: String): String = form.replace("|", "\\|")

  private fun preservedString(
    view: ExportTranslationView,
    key: String,
  ): String? = view.key.custom?.get(key) as? String

  private fun preservedNumber(
    view: ExportTranslationView,
    key: String,
  ): Number? = view.key.custom?.get(key) as? Number

  private fun preservedBoolean(
    view: ExportTranslationView,
    key: String,
  ): Boolean? = view.key.custom?.get(key) as? Boolean

  companion object {
    const val DEFAULT_COLLECTION_NAME = "Localization"
  }
}
