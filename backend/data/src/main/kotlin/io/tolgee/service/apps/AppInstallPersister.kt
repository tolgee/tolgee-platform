package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppInstallPersister(
  private val appInstallRepository: AppInstallRepository,
  private val appEnablementService: AppEnablementService,
  private val appService: AppService,
  private val appInstallPrincipalService: AppInstallPrincipalService,
  private val appsLimitGuard: AppsLimitGuard,
  private val appRegisterInserter: AppRegisterInserter,
  private val entityManager: EntityManager,
) : Logging {
  /**
   * Registers the app - making [organizationId] its owner - and, when [install] is true, installs it
   * in the same transaction, so a failed install never leaves behind an app its owner has no install
   * of. Registering an app somebody already registered is refused: installing an existing app is the
   * separate install endpoint. A concurrent duplicate loses the unique-constraint race, and the
   * non-transactional caller ([AppInstallService.register]) turns that into the same outcome.
   */
  @Transactional
  fun registerAndMaybeInstall(
    organizationId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
    install: Boolean,
  ): AppInstallService.RegisterAppResult {
    if (appService.find(fetched.manifest.id) != null) {
      throw BadRequestException(Message.APP_ALREADY_REGISTERED)
    }
    appsLimitGuard.checkAppsLimit(organizationId, registersNewApp = true)
    val inserted = appRegisterInserter.insert(organizationId, manifestUrl, fetched)
    val app = inserted.app
    if (!install) {
      return AppInstallService.RegisterAppResult(
        app = appService.summarize(app),
        appEntityId = app.id,
        appCredentials = inserted.credentials,
        install = null,
      )
    }
    val installEntity = persist(app, organizationId, fetched)
    return AppInstallService.RegisterAppResult(
      app = appService.summarize(app),
      appEntityId = app.id,
      appCredentials = inserted.credentials,
      install = installEntity,
    )
  }

  /** Installs an already-registered app. Discloses no app-level credentials. */
  @Transactional
  fun create(
    appEntityId: Long,
    organizationId: Long,
    fetched: AppManifestFetcher.FetchResult,
  ): AppInstallService.RegisterResult {
    appsLimitGuard.checkAppsLimit(organizationId, registersNewApp = false)
    val app = entityManager.getReference(App::class.java, appEntityId)
    val install = persist(app, organizationId, fetched)
    return AppInstallService.RegisterResult(
      install = install,
      app = appService.summarize(app),
      appCredentials = null,
    )
  }

  private fun persist(
    app: App,
    organizationId: Long,
    fetched: AppManifestFetcher.FetchResult,
  ): AppInstall {
    if (findByAppId(organizationId, fetched.manifest.id) != null) {
      throw BadRequestException(Message.APP_ALREADY_INSTALLED)
    }

    val install =
      AppInstall().apply {
        this.app = app
        this.organization = entityManager.getReference(Organization::class.java, organizationId)
        this.principal = appInstallPrincipalService.create(fetched.manifest.name)
        this.grantedScopes = fetched.scopes.toTypedArray()
      }

    return try {
      appInstallRepository.saveAndFlush(install)
    } catch (e: DataIntegrityViolationException) {
      // A concurrent install of the same app by this organization lost the unique-constraint race.
      logger.debug("Concurrent app install collided on the unique constraint", e)
      throw BadRequestException(Message.APP_ALREADY_INSTALLED)
    }
  }

  @Transactional
  fun remove(
    organizationId: Long,
    installId: Long,
  ) {
    val install =
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val principal = install.principal
    appEnablementService.removeAllForAppInstall(installId)
    appInstallRepository.delete(install)
    appInstallRepository.flush()
    appInstallPrincipalService.retire(principal)
  }

  private fun findByAppId(
    organizationId: Long,
    appId: String,
  ): AppInstall? {
    return appInstallRepository.findByOrganizationIdAndManifestAppId(organizationId, appId)
  }
}
