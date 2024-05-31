package io.tolgee.formats.xliff.`in`

import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.xliff.model.XliffFile
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
          addTranslation(source, transUnitId, file.sourceLanguageOrUnknown)
        }

        transUnit.target?.let { target ->
          addTranslation(target, transUnitId, file.targetLanguageOrUnknown)
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

  private fun addTranslation(
    text: String,
    transUnitId: String,
    language: String,
  ) {
    val converted = convertMessage(text, language)
    context.addTranslation(
      transUnitId,
      language,
      converted.message,
      pluralArgName = converted.pluralArgName,
      rawData = text,
      convertedBy = format,
    )
  }

  private val XliffFile.sourceLanguageOrUnknown: String
    get() = this.sourceLanguage ?: "unknown"

  private val XliffFile.targetLanguageOrUnknown: String
    get() = this.targetLanguage ?: "unknown"

  fun convertMessage(
    rawData: String,
    languageTag: String,
  ): MessageConvertorResult {
    return convertor.convert(
      rawData,
      languageTag,
      convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
      isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
    )
  }

  private val format by lazy {
    context.mapping?.format ?: XliffImportFormatDetector().detectFormat(parsed)
  }

  private val convertor by lazy {
    format.messageConvertor
  }
}
