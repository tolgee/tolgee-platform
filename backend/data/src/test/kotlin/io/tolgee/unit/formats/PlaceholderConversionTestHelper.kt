package io.tolgee.unit.formats

import io.tolgee.service.dataImport.CoreImportFilesProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil

object PlaceholderConversionTestHelper {
  fun testFile(
    fileName: String,
    resourcesFilePath: String,
    assertBeforeSettingsApplication: List<String>,
    assertAfterDisablingConversion: List<String>,
    assertAfterReEnablingConversion: List<String>,
  ) {
    val (beforeSettingsApplication, afterDisablingConversion, afterReEnablingConversion) =
      getResults(fileName, resourcesFilePath)

    try {
      beforeSettingsApplication.assert.isEqualTo(assertBeforeSettingsApplication)
      afterDisablingConversion.assert.isEqualTo(assertAfterDisablingConversion)
      afterReEnablingConversion.assert.isEqualTo(assertAfterReEnablingConversion)
    } catch (e: AssertionError) {
      throw AssertionError(
        """
        |${e.message}
        |
        |----------------
        |
        |Maybe you need to update the expected results:
        |${generateTest(fileName, resourcesFilePath)}
        """.trimMargin(),
        e,
      )
    }
  }

  private fun getResults(
    fileName: String,
    resourcesFilePath: String,
  ): Triple<List<String?>, List<String?>, List<String?>> {
    val mockUtil = FileProcessorContextMockUtil()

    val processor =
      mockUtil.mockCoreProcessor(
        fileName = fileName,
        resourcesFilePath = resourcesFilePath,
      )
    processor.processFiles(listOf(mockUtil.importFileDto))
    val beforeSettingsApplication = getSavedTranslationsAndClearMock(mockUtil)

    applyDisableConversion(processor, mockUtil)
    val afterDisablingConversion = getSavedTranslationsAndClearMock(mockUtil)

    applyEnableConversion(processor, mockUtil)
    val afterReEnablingConversion = getSavedTranslationsAndClearMock(mockUtil)
    return Triple(beforeSettingsApplication, afterDisablingConversion, afterReEnablingConversion)
  }

  private fun applyEnableConversion(
    processor: CoreImportFilesProcessor,
    mockUtil: FileProcessorContextMockUtil,
  ) {
    processor.importDataManager.applySettings(
      mockUtil.getImportSettings(convertPlaceholders = false),
      mockUtil.getImportSettings(convertPlaceholders = true),
    )
  }

  private fun applyDisableConversion(
    processor: CoreImportFilesProcessor,
    mockUtil: FileProcessorContextMockUtil,
  ) {
    processor.importDataManager.applySettings(
      mockUtil.getImportSettings(convertPlaceholders = true),
      mockUtil.getImportSettings(convertPlaceholders = false),
    )
  }

  private fun getSavedTranslationsAndClearMock(mockUtil: FileProcessorContextMockUtil): List<String?> {
    val savedTranslations = mockUtil.getSavedTranslations().map { it.text }
    mockUtil.clearImportServiceMockInvocation()
    return savedTranslations
  }

  private fun generateTest(
    fileName: String,
    resourceFilePath: String,
  ): String {
    val (beforeSettingsApplication, afterDisablingConversion, afterReEnablingConversion) =
      getResults(
        fileName,
        resourceFilePath,
      )
    val stringBuilder = StringBuilder()
    val indent = { i: Int -> " ".repeat(i * 2) }
    val escapeString = { s: String? -> s?.replace("\n", "\\n") }
    stringBuilder.append("${indent(2)}PlaceholderConversionTestHelper.testFile(\n")
    stringBuilder.append("${indent(4)}\"$fileName\",\n")
    stringBuilder.append("${indent(4)}\"$resourceFilePath\",\n")
    stringBuilder.append("${indent(4)}assertBeforeSettingsApplication = listOf(\n")
    beforeSettingsApplication.forEach {
      stringBuilder.append("${indent(6)}\"${escapeString(it)}\",\n")
    }
    stringBuilder.append("${indent(4)}),\n")
    stringBuilder.append("${indent(4)}assertAfterDisablingConversion = listOf(\n")
    afterDisablingConversion.forEach {
      stringBuilder.append("${indent(6)}\"${escapeString(it)}\",\n")
    }
    stringBuilder.append("${indent(4)}),\n")
    stringBuilder.append("${indent(4)}assertAfterReEnablingConversion = listOf(\n")
    afterReEnablingConversion.forEach {
      stringBuilder.append("${indent(6)}\"${escapeString(it)}\",\n")
    }
    stringBuilder.append("${indent(4)}),\n")
    stringBuilder.append("${indent(2)})")
    return stringBuilder.toString()
  }
}
