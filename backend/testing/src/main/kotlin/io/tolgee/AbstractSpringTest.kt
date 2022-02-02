package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.repository.EmailVerificationRepository
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.ApiKeyService
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.ImageUploadService
import io.tolgee.service.InvitationService
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.OrganizationService
import io.tolgee.service.PermissionService
import io.tolgee.service.ProjectService
import io.tolgee.service.ScreenshotService
import io.tolgee.service.TagService
import io.tolgee.service.TranslationCommentService
import io.tolgee.service.TranslationService
import io.tolgee.service.UserAccountService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.testing.AbstractTransactionalTest
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.transaction.annotation.Transactional

@Transactional
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
  protected lateinit var tolgeeProperties: TolgeeProperties

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
  protected lateinit var organizationService: OrganizationService

  @Autowired
  protected lateinit var organizationRoleService: OrganizationRoleService

  @Autowired
  lateinit var organizationRoleRepository: OrganizationRoleRepository

  @Autowired
  lateinit var projectRepository: ProjectRepository

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
  lateinit var machineTranslationProperties: MachineTranslationProperties

  @Autowired
  lateinit var awsMachineTranslationProperties: AwsMachineTranslationProperties

  @Autowired
  lateinit var googleMachineTranslationProperties: GoogleMachineTranslationProperties

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
  lateinit var mtCreditBucketService: MtCreditBucketService

  @Autowired
  lateinit var mtService: MtService

  @Autowired
  lateinit var mtServiceManager: MtServiceManager

  @Autowired
  private fun initInitialUser(authenticationProperties: AuthenticationProperties) {
    initialUsername = authenticationProperties.initialUsername
    initialPassword = initialPasswordManager.initialPassword
  }

  protected fun initMachineTranslationProperties(freeCreditsAmount: Long) {
    machineTranslationProperties.freeCreditsAmount = freeCreditsAmount
    awsMachineTranslationProperties.accessKey = "dummy"
    awsMachineTranslationProperties.defaultEnabled = false
    awsMachineTranslationProperties.secretKey = "dummy"
    googleMachineTranslationProperties.apiKey = "dummy"
    googleMachineTranslationProperties.defaultEnabled = true
    internalProperties.fakeMtProviders = false
  }
}
