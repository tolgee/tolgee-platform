package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.model.key.KeyMeta

data class FileProcessorContext(
  val file: ImportFileDto,
  val fileEntity: ImportFile,
  val maxTranslationTextLength: Long = 200L,
  val params: ImportAddFilesParams = ImportAddFilesParams(),
) {
  var languages: MutableMap<String, ImportLanguage> = mutableMapOf()
  var translations: MutableMap<String, MutableList<ImportTranslation>> = mutableMapOf()
  val keys: MutableMap<String, ImportKey> = mutableMapOf()
  var namespace: String? = null
  lateinit var languageNameGuesses: List<String>

  fun addTranslation(
    keyName: String,
    languageName: String,
    value: Any?,
    idx: Int = 0,
    isPlural: Boolean = false,
  ) {
    val stringValue = value as? String

    if (!validateAndSaveIssues(keyName, idx, value, stringValue)) return

    val language = getOrCreateLanguage(languageName)

    if (translations[keyName] == null) {
      translations[keyName] = mutableListOf()
    }

    if (value != null) {
      val entity = ImportTranslation(stringValue, language).also { it._isPlural = isPlural }
      translations[keyName]!!.add(entity)
    }
  }

  fun addPluralTranslationReplacingNonPlurals(
    keyName: String,
    languageName: String,
    value: Any?,
    idx: Int = 0,
  ) {
    val stringValue = value as? String

    if (!validateAndSaveIssues(keyName, idx, value, stringValue)) return

    val language = getOrCreateLanguage(languageName)

    if (translations[keyName] == null) {
      translations[keyName] = mutableListOf()
    }

    if (value != null) {
      val entity = ImportTranslation(stringValue, language).also { it._isPlural = true }
      translations[keyName]!!.removeIf { it.language == language && !it._isPlural }
      translations[keyName]!!.add(entity)
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

  fun addKeyComment(
    key: String,
    text: String?,
  ) {
    if (text.isNullOrBlank()) {
      return
    }
    val keyMeta = getOrCreateKeyMeta(key)
    keyMeta.addComment {
      this.text = text
    }
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
}
