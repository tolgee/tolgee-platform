package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.TranslationIssueType
import io.tolgee.model.key.KeyMeta

data class FileProcessorContext(
        val file: ImportFileDto,
        val fileEntity: ImportFile,
        val messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit,
) {
    var languages: MutableMap<String, ImportLanguage> = mutableMapOf()
    var translations: MutableMap<String, MutableList<ImportTranslation>> = mutableMapOf()
    val keys: MutableMap<String, ImportKey> = mutableMapOf()

    lateinit var languageNameGuesses: List<String>

    fun addTranslation(keyName: String, languageName: String, value: Any?) {
        val language = languages[languageName] ?: ImportLanguage(languageName, fileEntity).also {
            languages[languageName] = it
        }

        if (translations[keyName] == null) {
            translations[keyName] = mutableListOf()
        }

        val entity = ImportTranslation(value as? String?, language)

        translations[keyName]!!.add(entity)

        if (value != null && value !is String) {
            entity.addIssue(TranslationIssueType.VALUE_NOT_STRING)
        }
    }

    fun addKeyComment(key: String, text: String) {
        val keyMeta = getOrCreateKeyMeta(key)
        keyMeta.addComment {
            this.text = text
        }
    }

    fun addKeyCodeReference(key: String, path: String, line: Long) {
        val keyMeta = getOrCreateKeyMeta(key)
        keyMeta.addCodeReference {
            this.path = path
            this.line = line
        }
    }

    private fun getOrCreateKey(name: String): ImportKey {
        return keys[name] ?: let { ImportKey(name).also { keys[name] = it } }
    }

    private fun getOrCreateKeyMeta(key: String): KeyMeta {
        val keyEntity = getOrCreateKey(key)
        return keyEntity.keyMeta ?: let {
            keyEntity.keyMeta = KeyMeta(importKey = keyEntity)
            keyEntity.keyMeta!!
        }
    }
}
