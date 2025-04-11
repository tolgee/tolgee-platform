package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.ActivityService
import io.tolgee.component.AllCachesProvider
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.machineTranslation.*
import io.tolgee.constants.MtServiceType
import io.tolgee.development.DbPopulatorReal
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.repository.EmailVerificationRepository
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.ImageUploadService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditBucketService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.MfaService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.security.UserPreferencesService
import io.tolgee.service.translation.TranslationCommentService
import io.tolgee.service.translation.TranslationService
import io.tolgee.testing.AbstractTransactionalTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
abstract class AbstractSpringTest : AbstractTransactionalTest() {
  @Autowired
  protected lateinit var dbPopulator: DbPopulatorReal

  @Autowired
  protected lateinit var projectService: ProjectService

  @Autowired
  protected lateinit var translationService: TranslationService

  @Autowired
  protected lateinit var keyService: KeyService

  @Autowired
  protected lateinit var languageService: LanguageService

  @Autowired
  protected lateinit var keyRepository: KeyRepository

  @Autowired
  protected lateinit var userAccountService: UserAccountService

  @Autowired
  protected lateinit var apiKeyService: ApiKeyService

  @Autowired
  protected lateinit var permissionService: PermissionService

  @Autowired
  protected lateinit var invitationService: InvitationService

  @Autowired
  open lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  lateinit var mapper: ObjectMapper

  @Autowired
  protected lateinit var initialPasswordManager: InitialPasswordManager

  @Autowired
  protected lateinit var screenshotService: ScreenshotService

  @Autowired
  protected lateinit var imageUploadService: ImageUploadService

  protected lateinit var initialUsername: String

  protected lateinit var initialPassword: String

  @Autowired
  protected lateinit var organizationRepository: OrganizationRepository

  @Autowired
  open lateinit var organizationService: OrganizationService

  @Autowired
  protected lateinit var organizationRoleService: OrganizationRoleService

  @Autowired
  open lateinit var organizationRoleRepository: OrganizationRoleRepository

  @Autowired
  open lateinit var projectRepository: ProjectRepository

  @Autowired
  lateinit var importService: ImportService

  @Autowired
  lateinit var testDataService: TestDataService

  @Autowired
  lateinit var translationCommentService: TranslationCommentService

  @Autowired
  lateinit var tagService: TagService

  @Autowired
  lateinit var fileStorage: FileStorage

  @Autowired
  open lateinit var machineTranslationProperties: MachineTranslationProperties

  @Autowired
  lateinit var awsMachineTranslationProperties: AwsMachineTranslationProperties

  @Autowired
  lateinit var googleMachineTranslationProperties: GoogleMachineTranslationProperties

  @Autowired
  lateinit var deeplMachineTranslationProperties: DeeplMachineTranslationProperties

  @Autowired
  lateinit var azureCognitiveTranslationProperties: AzureCognitiveTranslationProperties

  @Autowired
  lateinit var baiduMachineTranslationProperties: BaiduMachineTranslationProperties

  @Autowired
  lateinit var llmProperties: LLMProperties

  @Autowired
  lateinit var internalProperties: InternalProperties

  @Autowired
  lateinit var mtServiceConfigService: MtServiceConfigService

  @set:Autowired
  lateinit var emailVerificationService: EmailVerificationService

  @set:Autowired
  lateinit var emailVerificationRepository: EmailVerificationRepository

  @set:Autowired
  lateinit var applicationContext: ApplicationContext

  @Autowired
  open lateinit var mtCreditBucketService: MtCreditBucketService

  @Autowired
  open lateinit var mtService: MtService

  @Autowired
  lateinit var mtServiceManager: MtServiceManager

  @Autowired
  lateinit var activityService: ActivityService

  @Autowired
  lateinit var userPreferencesService: UserPreferencesService

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  lateinit var languageStatsService: LanguageStatsService

  @Autowired
  lateinit var platformTransactionManager: PlatformTransactionManager

  @Autowired
  lateinit var patService: PatService

  @Autowired
  lateinit var mfaService: MfaService

  @Autowired
  lateinit var namespaceService: NamespaceService

  @Autowired
  open lateinit var cacheManager: CacheManager

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @Autowired
  lateinit var allCachesProvider: AllCachesProvider

  @BeforeEach
  fun clearCaches() {
    allCachesProvider.getAllCaches().forEach { cacheName ->
      cacheManager.getCache(cacheName)?.clear()
    }
  }

  @Autowired
  private fun initInitialUser(authenticationProperties: AuthenticationProperties) {
    initialUsername = authenticationProperties.initialUsername
    initialPassword = initialPasswordManager.initialPassword
  }

  protected fun initMachineTranslationProperties(
    freeCreditsAmount: Long,
    enabledServices: Set<MtServiceType> = setOf(MtServiceType.GOOGLE),
  ) {
    machineTranslationProperties.freeCreditsAmount = freeCreditsAmount
    awsMachineTranslationProperties.accessKey = "dummy"
    awsMachineTranslationProperties.defaultEnabled = enabledServices.contains(MtServiceType.AWS)
    awsMachineTranslationProperties.secretKey = "dummy"
    googleMachineTranslationProperties.apiKey = "dummy"
    googleMachineTranslationProperties.defaultEnabled = enabledServices.contains(MtServiceType.GOOGLE)
    deeplMachineTranslationProperties.defaultEnabled = enabledServices.contains(MtServiceType.DEEPL)
    deeplMachineTranslationProperties.authKey = "dummy"
    azureCognitiveTranslationProperties.defaultEnabled = enabledServices.contains(MtServiceType.AZURE)
    azureCognitiveTranslationProperties.authKey = "dummy"
    baiduMachineTranslationProperties.defaultEnabled = enabledServices.contains(MtServiceType.BAIDU)
    baiduMachineTranslationProperties.appId = "dummy"
    baiduMachineTranslationProperties.appSecret = "dummy"
    internalProperties.fakeMtProviders = false
  }

  fun <T> executeInNewTransaction(fn: (ts: TransactionStatus) -> T): T {
    return io.tolgee.util.executeInNewTransaction(
      transactionManager = platformTransactionManager,
      fn = fn,
      isolationLevel = TransactionDefinition.ISOLATION_DEFAULT,
    )
  }

  open fun setForcedDate(date: Date = Date()) {
    currentDateProvider.forcedDate = date
  }

  open fun clearForcedDate() {
    currentDateProvider.forcedDate = null
  }

  open fun forceDateString(
    dateString: String,
    pattern: String = "yyyy-MM-dd HH:mm:ss z",
  ) {
    currentDateProvider.forceDateString(dateString, pattern)
  }

  open fun moveCurrentDate(duration: Duration) {
    currentDateProvider.move(duration)
  }
}
