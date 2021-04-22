package io.tolgee.service.dataImport.processors

import io.tolgee.exceptions.FileIssueException
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import org.springframework.stereotype.Component

@Component
class ProcessorFactory {
    fun getArchiveProcessorByMimeType(type: String): ImportArchiveProcessor {
        return when (type) {
            "application/zip" -> ZipTypeProcessor()
            else -> throw FileIssueException(FileIssueType.NO_MATCHING_PROCESSOR)
        }
    }

    fun getProcessorByMimeType(type: String, context: FileProcessorContext): ImportFileProcessor {
        return when (type) {
            "application/json" -> JsonFileProcessor(context)
            else -> throw FileIssueException(FileIssueType.NO_MATCHING_PROCESSOR)
        }
    }
}
