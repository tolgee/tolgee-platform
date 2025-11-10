package io.tolgee.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.api.IImportSettings
import io.tolgee.component.KeyCustomValuesValidator
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.ImportFileProcessorFactory
import io.tolgee.model.Project
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.service.dataImport.CoreImportFilesProcessor
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.language.LanguageService
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext
import java.io.File
import kotlin.reflect.jvm.javaMethod

class FileProcessorContextMockUtil {
  private lateinit var importMock: Import
  lateinit var importFile: ImportFile
  lateinit var importFileDto: ImportFileDto
  lateinit var fileProcessorContext: FileProcessorContext
  lateinit var importServiceMock: ImportService
  lateinit var coreImportFileProcessor: CoreImportFilesProcessor
  val params = ImportAddFilesParams()

  fun mockIt(
    fileName: String,
    resourcesFilePath: String,
    convertPlaceholders: Boolean = true,
    projectIcuPlaceholdersEnabled: Boolean = true,
  ) {
    initImportMocks(fileName, resourcesFilePath)

    val applicationContextMock: ApplicationContext = mockApplicationContext()

    fileProcessorContext =
      FileProcessorContext(
        importFileDto,
        importFile,
        params = ImportAddFilesParams(),
        applicationContext = applicationContextMock,
        importSettings = getImportSettings(convertPlaceholders),
        projectIcuPlaceholdersEnabled = projectIcuPlaceholdersEnabled,
      )
  }

  fun getImportSettings(convertPlaceholders: Boolean) =
    object : IImportSettings {
      override var overrideKeyDescriptions: Boolean = false
      override var convertPlaceholdersToIcu: Boolean = convertPlaceholders
      override var createNewKeys: Boolean = true
    }

  private fun mockApplicationContext(): ApplicationContext {
    val applicationContextMock: ApplicationContext =
      Mockito.mock(ApplicationContext::class.java, Mockito.RETURNS_DEEP_STUBS)
    mockImportService(applicationContextMock)
    mockTolgeeProperties(applicationContextMock)
    mockImportFileProcessorFactory(applicationContextMock)
    mockLanguageService(applicationContextMock)
    mockKeyMetaService(applicationContextMock)
    val validator = Mockito.mock(KeyCustomValuesValidator::class.java)
    Mockito.`when`(applicationContextMock.getBean(KeyCustomValuesValidator::class.java)).thenReturn(validator)
    Mockito.`when`(validator.isValid(any())).thenReturn(true)
    return applicationContextMock
  }

  private fun mockKeyMetaService(applicationContextMock: ApplicationContext) {
    val keyMetaService = mock<KeyMetaService>()
    whenever(applicationContextMock.getBean(KeyMetaService::class.java))
      .thenReturn(keyMetaService)
  }

  private fun mockLanguageService(applicationContextMock: ApplicationContext) {
    val languageService = mock<LanguageService>()
    whenever(applicationContextMock.getBean(LanguageService::class.java))
      .thenReturn(languageService)
  }

  private fun mockImportFileProcessorFactory(applicationContextMock: ApplicationContext): ImportFileProcessorFactory {
    val tolgeeProps = mockTolgeeProperties(applicationContextMock)
    return ImportFileProcessorFactory(
      objectMapper = jacksonObjectMapper(),
      yamlObjectMapper = ObjectMapper(YAMLFactory()),
      tolgeeProperties = tolgeeProps,
    ).also {
      whenever(applicationContextMock.getBean(ImportFileProcessorFactory::class.java))
        .thenReturn(it)
    }
  }

  private fun mockImportService(applicationContextMock: ApplicationContext): ImportService {
    return mock<ImportService>().also {
      whenever(applicationContextMock.getBean(ImportService::class.java))
        .thenReturn(it)
      mockSaveFile(it)
      importServiceMock = it
    }
  }

  private fun mockSaveFile(it: ImportService) {
    whenever(it.saveFile(any())).then { it.arguments[0] as ImportFile }
  }

  private fun mockTolgeeProperties(applicationContextMock: ApplicationContext): TolgeeProperties {
    return TolgeeProperties().also {
      whenever(applicationContextMock.getBean(TolgeeProperties::class.java))
        .thenReturn(it)
    }
  }

  private fun initImportMocks(
    fileName: String,
    resourcesFilePath: String,
  ) {
    importMock = Import(project = Project())
    importFile = ImportFile(fileName, importMock)
    importFileDto =
      ImportFileDto(
        fileName,
        File(resourcesFilePath)
          .readBytes(),
      )
  }

  fun mockCoreProcessor(
    fileName: String,
    resourcesFilePath: String,
  ): CoreImportFilesProcessor {
    val applicationContext = mockApplicationContext()
    initImportMocks(fileName, resourcesFilePath)
    return CoreImportFilesProcessor(
      applicationContext = applicationContext,
      import = importMock,
      importSettings = getImportSettings(true),
    ).also { coreImportFileProcessor = it }
  }

  fun getSavedTranslations(): List<ImportTranslation> {
    @Suppress("UNCHECKED_CAST")
    return Mockito
      .mockingDetails(importServiceMock)
      .invocations
      .filter { it.method == ImportService::saveTranslations.javaMethod }
      .flatMap { it.arguments.first() as List<ImportTranslation> }
  }

  fun clearImportServiceMockInvocation() {
    Mockito.clearInvocations(importServiceMock)
  }
}
