package io.tolgee.unit

import io.tolgee.component.KeyCustomValuesValidator
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.dataImport.ImportSettingsRequest
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.ImportFileProcessorFactory
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.dataImport.CoreImportFilesProcessor
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext

class CoreImportFileProcessorUnitTest {
  private lateinit var existingLanguageEntity: Language
  private lateinit var applicationContextMock: ApplicationContext
  private lateinit var importMock: Import
  private lateinit var importFileProcessorFactoryMock: ImportFileProcessorFactory
  private lateinit var importServiceMock: ImportService
  private lateinit var languageServiceMock: LanguageService
  private lateinit var processor: CoreImportFilesProcessor
  private lateinit var typeProcessorMock: ImportFileProcessor
  private lateinit var fileProcessorContext: FileProcessorContext
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var translationServiceMock: TranslationService
  private lateinit var existingLanguage: LanguageDto
  private lateinit var existingTranslation: Translation
  private lateinit var authenticationFacadeMock: AuthenticationFacade
  private lateinit var keyMetaServiceMock: KeyMetaService
  private lateinit var tolgeePropertiesMock: TolgeeProperties
  private lateinit var keyCustomValuesValidatorMock: KeyCustomValuesValidator

  @BeforeEach
  fun setup() {
    applicationContextMock = mock()
    importMock = mock()
    importFileProcessorFactoryMock = mock()
    importServiceMock = mock()
    languageServiceMock = mock()
    translationServiceMock = mock()
    typeProcessorMock = mock()
    authenticationFacadeMock = mock()
    keyMetaServiceMock = mock()
    tolgeePropertiesMock = mock()
    keyCustomValuesValidatorMock = mock()

    importFile = ImportFile("lgn.json", importMock)
    importFileDto = ImportFileDto("lng.json", "".toByteArray())
    existingLanguage = LanguageDto(name = "lng")
    existingLanguageEntity = Language().apply { name = existingLanguage.name }
    existingTranslation = Translation("helllo").also { it.key = Key(name = "colliding key") }
    processor =
      CoreImportFilesProcessor(
        applicationContextMock,
        importMock,
        importSettings =
          ImportSettingsRequest(
            overrideKeyDescriptions = false,
            convertPlaceholdersToIcu = true,
            createNewKeys = false,
          ),
      )

    whenever(applicationContextMock.getBean(ImportFileProcessorFactory::class.java)).thenReturn(
      importFileProcessorFactoryMock,
    )
    whenever(applicationContextMock.getBean(ImportService::class.java)).thenReturn(importServiceMock)
    whenever(applicationContextMock.getBean(LanguageService::class.java)).thenReturn(languageServiceMock)
    whenever(applicationContextMock.getBean(TranslationService::class.java)).thenReturn(translationServiceMock)
    whenever(applicationContextMock.getBean(AuthenticationFacade::class.java)).thenReturn(authenticationFacadeMock)
    whenever(applicationContextMock.getBean(KeyMetaService::class.java)).thenReturn(keyMetaServiceMock)
    whenever(applicationContextMock.getBean(TolgeeProperties::class.java)).thenReturn(tolgeePropertiesMock)
    whenever(applicationContextMock.getBean(KeyCustomValuesValidator::class.java))
      .thenReturn(keyCustomValuesValidatorMock)
    whenever(tolgeePropertiesMock.maxTranslationTextLength).then { 10000L }

    whenever(importFileProcessorFactoryMock.getProcessor(eq(importFileDto), any())).thenReturn(typeProcessorMock)
    fileProcessorContext = FileProcessorContext(importFileDto, importFile, applicationContext = applicationContextMock)
    fileProcessorContext.languages = mutableMapOf("lng" to ImportLanguage("lng", importFile))
    whenever(typeProcessorMock.context).then { fileProcessorContext }
    whenever(importMock.project).thenReturn(Project(1, "test repo"))
    whenever(importServiceMock.saveFile(any())).thenReturn(importFile)
    whenever(languageServiceMock.findByTag(eq("lng"), any<Long>()))
      .thenReturn(existingLanguage)
    whenever(languageServiceMock.getEntity(any<Long>()))
      .thenReturn(existingLanguageEntity)
    val userAccount = UserAccount()
    whenever(authenticationFacadeMock.authenticatedUser).thenReturn(UserAccountDto.fromEntity(userAccount))
    whenever(authenticationFacadeMock.authenticatedUserEntity).thenReturn(userAccount)
  }

  @Test
  fun `finds proper existing language for imported language`() {
    processor.processFiles(listOf(importFileDto))
    verify(importServiceMock).saveLanguages(
      argThat {
        this.first().run {
          name == "lng" && existingLanguage!!.name == "lng"
        }
      },
    )
  }

  @Test
  fun `handles conflicts properly`() {
    fileProcessorContext.addTranslation("colliding key", "lng", "colliding value")
    fileProcessorContext.addTranslation("not colliding key", "lng", "not colliding value")
    fileProcessorContext.addTranslation("equal key", "lng", "equal text")

    whenever(translationServiceMock.getAllByLanguageId(any())).thenReturn(
      listOf(
        existingTranslation,
        Translation("equal text").also {
          it.key = Key("equal key")
        },
      ),
    )
    processor.processFiles(listOf(importFileDto))
    verify(importServiceMock).saveTranslations(
      argThat {
        assertThat(this[0].conflict).isEqualTo(existingTranslation)
        assertThat(this[1].conflict).isNull()
        assertThat(this[2].conflict).isNull()
        true
      },
    )
  }

  @Test
  fun `stores key meta`() {
    fileProcessorContext.addTranslation("test_key", "lng", "value")
    fileProcessorContext.addKeyCodeReference("test_key", "hello.php", 10)
    fileProcessorContext.addKeyCodeReference("test_key", "hello2.php", 10)
    fileProcessorContext.addKeyDescription("test_key", "test comment")
    whenever(translationServiceMock.getAllByLanguageId(any())).thenReturn(listOf())

    processor.processFiles(listOf(importFileDto))
    verify(keyMetaServiceMock).save(
      argThat {
        this.description == "test comment"
      },
    )
    verify(keyMetaServiceMock).save(
      argThat {
        this.codeReferences.any { it.path == "hello.php" }
      },
    )
    verify(importServiceMock).saveTranslations(
      argThat {
        assertThat(this[0].key.keyMeta).isNotNull
        assertThat(this[0].key.keyMeta?.codeReferences).hasSize(2)
        assertThat(this[0].key.keyMeta?.description).isNotBlank()
        true
      },
    )
  }
}
