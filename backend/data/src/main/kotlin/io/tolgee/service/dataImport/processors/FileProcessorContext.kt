package io.tolgee.service.dataImport.processors

import io.tolgee.api.IImportSettings
import io.tolgee.component.KeyCustomValuesValidator
import io.tolgee.dtos.dataImport.IImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.ImportMessageConvertorType
import io.tolgee.formats.getPluralForms
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.model.key.KeyMeta
import org.springframework.context.ApplicationContext

data class FileProcessorContext(
  var file: ImportFileDto,
  val fileEntity: ImportFile,
  val maxTranslationTextLength: Long = 200L,
  val params: IImportAddFilesParams = ImportAddFilesParams(),
  val importSettings: IImportSettings =
    object : IImportSettings {
      override var overrideKeyDescriptions: Boolean = false
      override var convertPlaceholdersToIcu: Boolean = true
    },
  val applicationContext: ApplicationContext,
) {
  var languages: MutableMap<String, ImportLanguage> = mutableMapOf()
  private var _translations: MutableMap<String, MutableList<ImportTranslation>> = mutableMapOf()
  val translations: Map<String, List<ImportTranslation>> get() = _translations
  val keys: MutableMap<String, ImportKey> = mutableMapOf()
  var namespace: String? = null
  lateinit var languageNameGuesses: List<String>
  var needsParamConversion = false

  /**
   * @param forceIsPlural when is set to true, it will force the translation to be plurar, when set to false,
   * it will force the translation not to be plural
   */
  fun addTranslation(
    keyName: String,
    languageName: String,
    value: Any?,
    idx: Int = 0,
    forceIsPlural: Boolean? = null,
    replaceNonPlurals: Boolean = false,
    rawData: Any? = null,
    convertedBy: ImportMessageConvertorType? = null,
  ) {
    val stringValue = value as? String

    if (!validateAndSaveIssues(keyName, idx, value, stringValue)) return

    val language = getOrCreateLanguage(languageName)

    if (_translations[keyName] == null) {
      _translations[keyName] = mutableListOf()
    }

    val isPlural = forceIsPlural ?: (getPluralForms(stringValue) != null)

    if (convertedBy != null) {
      needsParamConversion = true
    }

    if (value != null) {
      val entity =
        ImportTranslation(stringValue, language).also {
          it.isPlural = isPlural
          it.rawData = rawData
          it.convertor = convertedBy
        }
      if (isPlural && replaceNonPlurals) {
        _translations[keyName]!!.removeIf { it.language == language && !it.isPlural }
      }
      _translations[keyName]!!.add(entity)
    }
  }

  private fun validateAndSaveIssues(
    keyName: String,
    idx: Int,
    value: Any?,
    stringValue: String?,
  ): Boolean {
    if (keyName.isBlank()) {
      this.fileEntity.addKeyIsBlankIssue(idx)
      return false
    }

    if (value !is String?) {
      this.fileEntity.addValueIsNotStringIssue(keyName, idx, value)
      return false
    }

    if (value.isNullOrEmpty()) {
      this.fileEntity.addValueIsEmptyIssue(keyName)
    }

    if (stringValue != null && stringValue.length > maxTranslationTextLength) {
      fileEntity.addIssue(FileIssueType.TRANSLATION_TOO_LONG, mapOf(FileIssueParamType.KEY_NAME to keyName))
      return false
    }
    return true
  }

  private fun getOrCreateLanguage(languageName: String): ImportLanguage {
    return languages[languageName] ?: ImportLanguage(languageName, fileEntity).also {
      languages[languageName] = it
    }
  }

  fun addKeyDescription(
    key: String,
    text: String?,
  ) {
    if (text.isNullOrBlank()) {
      return
    }
    val keyMeta = getOrCreateKeyMeta(key)
    keyMeta.description = text.trim()
  }

  fun addKeyCodeReference(
    key: String,
    path: String,
    line: Long? = null,
  ) {
    val keyMeta = getOrCreateKeyMeta(key)
    keyMeta.addCodeReference {
      this.path = path
      this.line = line
    }
  }

  fun setCustom(
    translationKey: String,
    customMapKey: String,
    value: Any,
  ) {
    val keyMeta = getOrCreateKeyMeta(translationKey)
    keyMeta.setCustom(customMapKey, value)
    keyMeta.custom?.let {
      if (!customValuesValidator.isValid(it)) {
        keyMeta.custom?.remove(customMapKey)
        fileEntity.addIssue(FileIssueType.INVALID_CUSTOM_VALUES, mapOf(FileIssueParamType.KEY_NAME to translationKey))
      }
    }
  }

  private fun getOrCreateKey(name: String): ImportKey {
    return keys[name] ?: let { ImportKey(name, this.fileEntity).also { keys[name] = it } }
  }

  private fun getOrCreateKeyMeta(key: String): KeyMeta {
    val keyEntity = getOrCreateKey(key)
    return keyEntity.keyMeta ?: let {
      keyEntity.keyMeta = KeyMeta(importKey = keyEntity)
      keyEntity.keyMeta!!
    }
  }

  val customValuesValidator: KeyCustomValuesValidator by lazy {
    applicationContext.getBean(KeyCustomValuesValidator::class.java)
  }
}
