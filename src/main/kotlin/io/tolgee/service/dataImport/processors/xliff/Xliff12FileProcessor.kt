package io.tolgee.service.dataImport.processors.xliff

import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import javax.xml.stream.XMLStreamReader

class Xliff12FileProcessor(override val context: FileProcessorContext,
                           override val xmlStreamReader: XMLStreamReader
) : ImportFileProcessor() {

    val openElements = mutableListOf<String>()
    val currentOpenElement: String?
        get() = openElements.lastOrNull()

    override fun process() {
        var fileOriginal: String? = null
        var sourceLanguage: String? = null
        var targetLanguage: String? = null
        var id: String? = null
        var currentTextValue: String? = null
        var targetProvided = false

        while (xmlStreamReader.hasNext()) {
            xmlStreamReader.next()
            if (xmlStreamReader.isStartElement) {
                openElements.add(xmlStreamReader.localName.toLowerCase())
                when (currentOpenElement) {
                    "file" -> {
                        fileOriginal = xmlStreamReader.getAttributeValue(null, "original")
                        sourceLanguage = xmlStreamReader.getAttributeValue(null, "source-language")
                        targetLanguage = xmlStreamReader.getAttributeValue(null, "target-language")
                    }
                    "trans-unit" -> {
                        id = xmlStreamReader.getAttributeValue(null, "id")
                        if (id != null && fileOriginal != null) {
                            context.addKeyCodeReference(id, fileOriginal, null)
                        }
                        if (fileOriginal != null && id == null) {
                            context.fileEntity.addIssue(
                                    FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED,
                                    mapOf(FileIssueParamType.FILE_NODE_ORIGINAL to fileOriginal))
                        }
                    }
                }
            }
            if (xmlStreamReader.isCharacters) {
                if (currentOpenElement != null)
                    when (currentOpenElement!!) {
                        in "source", "target", "note" -> {
                            currentTextValue = (currentTextValue ?: "") + xmlStreamReader.text
                        }
                    }
            }

            if (xmlStreamReader.isEndElement) {
                when (currentOpenElement) {
                    "file" -> {
                        fileOriginal = null
                        sourceLanguage = null
                        targetLanguage = null
                    }
                    "trans-unit" -> {
                        if (id != null && !targetProvided) {
                            context.fileEntity.addIssue(
                                    FileIssueType.TARGET_NOT_PROVIDED,
                                    mapOf(FileIssueParamType.KEY_NAME to id)
                            )
                            id = null
                        }
                        targetProvided = false
                    }
                    "source" -> {
                        if (sourceLanguage != null && id != null) {
                            context.addTranslation(id, sourceLanguage, currentTextValue)
                        }
                    }
                    "target" -> {
                        if (targetLanguage != null && id != null) {
                            targetProvided = true
                            context.addTranslation(id, targetLanguage, currentTextValue)
                        }
                    }
                    "note" -> {
                        if (currentTextValue != null && currentTextValue.isNotBlank() && id != null) {
                            context.addKeyComment(id, currentTextValue)
                        }
                    }
                }
                currentTextValue = null
                openElements.removeLast()
            }
        }
    }
}
