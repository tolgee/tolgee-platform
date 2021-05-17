package io.tolgee.unit

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.Language
import io.tolgee.model.Repository
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.key.Key
import io.tolgee.service.LanguageService
import io.tolgee.service.TranslationService
import io.tolgee.service.dataImport.CoreImportFilesProcessor
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ImportFileProcessor
import io.tolgee.service.dataImport.processors.ProcessorFactory
import org.mockito.kotlin.*
import org.springframework.context.ApplicationContext
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.*

class CoreImportFileProcessorUnitTest {

    private lateinit var applicationContextMock: ApplicationContext
    private lateinit var importMock: Import
    private lateinit var processorFactoryMock: ProcessorFactory
    private lateinit var importServiceMock: ImportService
    private lateinit var languageServiceMock: LanguageService
    private lateinit var processor: CoreImportFilesProcessor
    private lateinit var typeProcessorMock: ImportFileProcessor
    private lateinit var fileProcessorContext: FileProcessorContext
    private lateinit var importFile: ImportFile
    private lateinit var importFileDto: ImportFileDto
    private lateinit var translationServiceMock: TranslationService
    private lateinit var existingLanguage: Language
    private lateinit var existingTranslation: Translation

    @BeforeMethod
    fun setup() {
        applicationContextMock = mock()
        importMock = mock()
        processorFactoryMock = mock()
        importServiceMock = mock()
        languageServiceMock = mock()
        translationServiceMock = mock()
        typeProcessorMock = mock()

        importFile = ImportFile("lgn.json", importMock)
        importFileDto = ImportFileDto("lng.json", "".toByteArray().inputStream())
        existingLanguage = Language().also { it.name = "lng" }
        existingTranslation = Translation(1, "helllo").also { it.key = Key(name = "colliding key") }
        processor = CoreImportFilesProcessor(applicationContextMock, importMock)
        whenever(applicationContextMock.getBean(ProcessorFactory::class.java)).thenReturn(processorFactoryMock)
        whenever(applicationContextMock.getBean(ImportService::class.java)).thenReturn(importServiceMock)
        whenever(applicationContextMock.getBean(LanguageService::class.java)).thenReturn(languageServiceMock)
        whenever(applicationContextMock.getBean(TranslationService::class.java)).thenReturn(translationServiceMock)
        whenever(processorFactoryMock.getProcessor(eq(importFileDto), any())).thenReturn(typeProcessorMock)
        fileProcessorContext = FileProcessorContext(importFileDto, importFile, mock())
        fileProcessorContext.languages = mutableMapOf("lng" to ImportLanguage("lng", importFile))
        whenever(typeProcessorMock.context).then { fileProcessorContext }
        whenever(importMock.repository).thenReturn(Repository(1, "test repo"))
        whenever(importServiceMock.saveFile(any())).thenReturn(importFile)
        whenever(languageServiceMock.findByAbbreviation(eq("lng"), any<Long>()))
                .thenReturn(Optional.of(existingLanguage))
    }

    @Test
    fun `finds proper existing language for imported language`() {
        processor.processFiles(listOf(importFileDto), messageClient = mock())
        verify(importServiceMock).saveLanguages(argThat {
            this.first().run {
                name == "lng" && existingLanguage!!.name == "lng"
            }
        })
    }

    @Test
    fun `handles conflicts properly`() {
        fileProcessorContext.addTranslation("colliding key", "lng", "colliding value")
        fileProcessorContext.addTranslation("not colliding key", "lng", "not colliding value")

        whenever(translationServiceMock.getAllByLanguageId(any())).thenReturn(setOf(existingTranslation))
        processor.processFiles(listOf(importFileDto), messageClient = mock())
        verify(importServiceMock).saveTranslations(argThat {
            assertThat(this[0].conflict).isEqualTo(existingTranslation)
            assertThat(this[1].conflict).isNull()
            true
        })
    }

    @Test
    fun `stores key meta`() {
        fileProcessorContext.addTranslation("test_key", "lng", "value")
        fileProcessorContext.addKeyCodeReference("test_key", "hello.php", 10)
        fileProcessorContext.addKeyCodeReference("test_key", "hello2.php", 10)
        fileProcessorContext.addKeyComment("test_key", "test comment")
        whenever(translationServiceMock.getAllByLanguageId(any())).thenReturn(setOf())

        processor.processFiles(listOf(importFileDto), messageClient = mock())
        verify(importServiceMock).saveTranslations(argThat {
            assertThat(this[0].key.keyMeta).isNotNull
            assertThat(this[0].key.keyMeta?.codeReferences).hasSize(2)
            assertThat(this[0].key.keyMeta?.comments).hasSize(1)
            true
        })
    }
}
