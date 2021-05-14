package io.tolgee.service.dataImport.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.FileIssueException
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.service.dataImport.processors.po.PoFileProcessor
import io.tolgee.service.dataImport.processors.xliff.XliffFileProcessor
import org.springframework.stereotype.Component

@Component
class ProcessorFactory {
    fun getArchiveProcessor(file: ImportFileDto): ImportArchiveProcessor {
        return when (file.name.fileNameExtension) {
            "zip" -> ZipTypeProcessor()
            else -> throw FileIssueException(FileIssueType.NO_MATCHING_PROCESSOR)
        }
    }

    fun getProcessor(file: ImportFileDto, context: FileProcessorContext): ImportFileProcessor {
        return when (file.name.fileNameExtension) {
            "json" -> JsonFileProcessor(context)
            "po" -> PoFileProcessor(context)
            "xliff" -> XliffFileProcessor(context)
            "xlf" -> XliffFileProcessor(context)
            else -> throw FileIssueException(FileIssueType.NO_MATCHING_PROCESSOR)
        }
    }

    val String?.fileNameExtension: String?
        get() {
            return this?.replace(".*\\.(.+)\$".toRegex(), "$1")
        }
}
