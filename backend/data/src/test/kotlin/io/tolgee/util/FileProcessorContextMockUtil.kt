package io.tolgee.util

import io.tolgee.component.KeyCustomValuesValidator
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.springframework.context.ApplicationContext
import java.io.File

class FileProcessorContextMockUtil {
  private lateinit var importMock: Import
  lateinit var importFile: ImportFile
  lateinit var importFileDto: ImportFileDto
  lateinit var fileProcessorContext: FileProcessorContext

  fun mockIt(
    fileName: String,
    resourcesFilePath: String,
  ) {
    importMock = mock()
    importFile = ImportFile(fileName, importMock)
    importFileDto =
      ImportFileDto(
        fileName,
        File(resourcesFilePath)
          .readBytes(),
      )

    val applicationContextMock: ApplicationContext =
      Mockito.mock(ApplicationContext::class.java, Mockito.RETURNS_DEEP_STUBS)
    val validator = Mockito.mock(KeyCustomValuesValidator::class.java)
    Mockito.`when`(applicationContextMock.getBean(KeyCustomValuesValidator::class.java)).thenReturn(validator)
    Mockito.`when`(validator.isValid(any())).thenReturn(true)
    fileProcessorContext = FileProcessorContext(importFileDto, importFile, applicationContext = applicationContextMock)
  }
}
