package io.tolgee.service.dataImport.processors

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType.KEY_IS_NOT_STRING
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType

class JsonFileProcessor(
        override val context: FileProcessorContext
) : ImportFileProcessor {
    override fun process() {
        try {
            val data = jacksonObjectMapper().readValue<Map<String, Any>>(context.file.inputStream)
            val parsed = data.parse()
            parsed.forEach {
                context.addTranslation(it.key, guessLanguageName(), it.value)
            }
        } catch (e: JsonParseException) {
            throw ImportCannotParseFileException(context.file.name, e.message)
        }
    }

    private fun Map<*, *>.parse(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        this.entries.forEach { entry ->
            (entry.key as? String)?.also { key ->
                (entry.value as? String)?.let { value ->
                    data[key] = value
                }
                (entry.value as? Map<*, *>)?.let { embedded ->
                    embedded.parse().forEach { embeddedEntry ->
                        data["$key.${embeddedEntry.key}"] = embeddedEntry.value
                    }
                }
            } ?: let {
                context.fileEntity.addIssue(KEY_IS_NOT_STRING, mapOf(FileIssueParamType.KEY to entry.key.toString()))
            }
        }
        return data
    }

    private fun guessLanguageName(): String {
        val guess = context.file.name!!.replace("^(.*?)\\..*".toRegex(), "$1")
        if (guess.isBlank()) {
            return this.context.file.name
        }
        return guess
    }

    companion object {
        data class ParseResult(
                val data: Map<String, Any>,
                val issues: List<ImportFileIssue>
        )
    }
}
