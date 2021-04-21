package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.TranslationIssueType

data class FileProcessorContext(
        val file: ImportFileDto,
        val fileEntity: ImportFile,
        val messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit,
) {
    var files: MutableList<ImportFileDto>? = null
    var languages: MutableMap<String, ImportLanguage> = mutableMapOf()
    var translations: MutableMap<String, MutableList<ImportTranslation>> = mutableMapOf()

    fun addTranslation(keyName: String, languageName: String, value: Any?) {
        val language = languages[languageName] ?: ImportLanguage(languageName, fileEntity).also {
            languages[languageName] = it
        }

        if (translations[keyName] == null) {
            translations[keyName] = mutableListOf()
        }

        val entity = ImportTranslation(value as? String?, language);

        translations[keyName]!!.add(entity)

        if (value != null && value !is String) {
            entity.addIssue(TranslationIssueType.VALUE_NOT_STRING)
        }
    }
}
