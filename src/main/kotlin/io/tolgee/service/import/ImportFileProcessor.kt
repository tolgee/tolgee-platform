package io.tolgee.service.import

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportFileProcessorResult
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_ARCHIVE
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_FILES_IN_ARCHIVE
import io.tolgee.exceptions.FileIssueException
import io.tolgee.model.import.Import
import io.tolgee.model.import.ImportArchive
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.issues.ImportFileIssue
import io.tolgee.model.import.issues.issueTypes.FileIssueType
import io.tolgee.service.import.processors.ImportFileTypeProcessor
import io.tolgee.service.import.processors.ZipTypeProcessor
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import java.net.FileNameMap
import java.net.URLConnection


@Component
@RequestScope
class ImportFileProcessor(
        private val zipTypeProcessor: ZipTypeProcessor,
        private val importService: ImportService
) {
    lateinit var import: Import

    fun processFiles(files: List<ImportFileDto>?,
                     messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit) {
        files?.forEach {
            processFiles(processFile(it, messageClient).files, messageClient)
        }
    }

    private fun processFile(file: ImportFileDto,
                            messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit)
            : ImportFileProcessorResult {
        try {
            val mimeType = file.getContentMimeType()
            val processor = getProcessorByMimeType(mimeType)

            if (isArchive(mimeType)) {
                messageClient(FOUND_ARCHIVE, null)
                file.saveArchiveEntity()
                return processor.process(file, messageClient).also {
                    messageClient(FOUND_FILES_IN_ARCHIVE, listOf(it.files?.size ?: 0))
                }
            }
            file.saveFileEntity()
            processor.process(file, messageClient)
        } catch (e: FileIssueException) {
            file.saveFileEntity().let { fileEntity ->
                importService.saveFileIssue(ImportFileIssue(file = fileEntity, type = e.type))
            }
        }

        return ImportFileProcessorResult()
    }

    private fun ImportFileDto.saveFileEntity() = importService.saveFile(ImportFile(this.name, import))

    private fun ImportFileDto.saveArchiveEntity() = importService.saveArchive(ImportArchive(this.name!!, import))


    private fun getProcessorByMimeType(type: String): ImportFileTypeProcessor {
        return when (type) {
            "application/zip" -> zipTypeProcessor
            else -> throw FileIssueException(FileIssueType.NO_MATCHING_PROCESSOR)
        }
    }

    private fun isArchive(mimeType: String) = mimeType == "application/zip"

    private fun ImportFileDto.getContentMimeType(): String {
        this.name?.let { filename ->
            if (filename.endsWith(".json")) {
                return "application/json"
            }
            val fileNameMap: FileNameMap = URLConnection.getFileNameMap()
            return fileNameMap.getContentTypeFor(filename)
                    ?: throw FileIssueException(FileIssueType.NO_MATCHING_PROCESSOR)
        } ?: throw FileIssueException(FileIssueType.NO_FILENAME_PROVIDED)
    }
}
