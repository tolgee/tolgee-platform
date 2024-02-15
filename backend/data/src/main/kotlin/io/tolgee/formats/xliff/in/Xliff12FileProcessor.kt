package io.tolgee.formats.xliff.`in`

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.dataImport.processors.FileProcessorContext

class Xliff12FileProcessor(
  override val context: FileProcessorContext,
  private val parsed: XliffModel,
) : ImportFileProcessor() {
  override fun process() {
    parsed.files.forEach { file ->
      file.transUnits.forEach transUnitsForeach@{ transUnit ->
        val fileOriginal = file.original
        val transUnitId =
          transUnit.id ?: let {
            context.fileEntity.addIssue(
              FileIssueType.ID_ATTRIBUTE_NOT_PROVIDED,
              mapOf(FileIssueParamType.FILE_NODE_ORIGINAL to (fileOriginal ?: "")),
            )
            return@transUnitsForeach
          }
        if (!fileOriginal.isNullOrBlank()) {
          context.addKeyCodeReference(transUnitId, fileOriginal, null)
        }
        transUnit.source?.let { source ->
          context.addTranslation(
            transUnitId,
            file.sourceLanguage ?: "unknown source",
            source,
            rawData = null,
          )
        }

        transUnit.target?.let { target ->
          context.addTranslation(
            transUnitId,
            file.targetLanguage ?: "unknown target",
            target,
            rawData = null,
          )
        } ?: let {
          context.fileEntity.addIssue(
            FileIssueType.TARGET_NOT_PROVIDED,
            mapOf(FileIssueParamType.KEY_NAME to transUnitId),
          )
        }

        transUnit.note?.let { context.addKeyDescription(transUnitId, it) }
      }
    }
  }
}
