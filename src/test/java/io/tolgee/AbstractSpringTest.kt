package io.tolgee

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.repository.RepositoryRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.*
import io.tolgee.service.dataImport.ImportService
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractSpringTest : AbstractTransactionalTest() {
    @Autowired
    protected lateinit var dbPopulator: DbPopulatorReal

    @Autowired
    protected lateinit var repositoryService: RepositoryService

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

    protected lateinit var initialUsername: String

    protected lateinit var initialPassword: String

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository

    @Autowired
    protected lateinit var organizationService: OrganizationService

    @Autowired
    protected lateinit var organizationRoleService: OrganizationRoleService

    @Autowired lateinit var organizationRoleRepository: OrganizationRoleRepository

    @Autowired lateinit var repositoryRepository: RepositoryRepository

    @Autowired lateinit var importService: ImportService

    @Autowired lateinit var testDataService: TestDataService

    @Autowired
    private fun initInitialUser(authenticationProperties: AuthenticationProperties) {
        initialUsername = authenticationProperties.initialUsername
        initialPassword = initialPasswordManager.initialPassword
    }
}
