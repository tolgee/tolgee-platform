package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.enums.Scope
import io.tolgee.repository.apps.AppInstallRepository
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
  private val entityManager: EntityManager,
) {
  /**
   * Registers the app if nobody has yet — making [organizationId] its owner — and installs it in one
   * transaction, so a failed install never leaves behind an app its owner has no install of.
   */
  @Transactional
  fun registerAndCreate(
    organizationId: Long,
    authorId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
  ): AppInstallService.RegisterResult {
    val resolved = appService.registerIfAbsent(organizationId, authorId, manifestUrl, fetched)
    return persist(resolved.app, organizationId, authorId, manifestUrl, fetched, resolved.credentials)
  }

  /** Installs an already-registered app. Discloses no app-level credentials. */
  @Transactional
  fun create(
    appEntityId: Long,
    organizationId: Long,
    authorId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
  ): AppInstallService.RegisterResult {
    val app = entityManager.getReference(App::class.java, appEntityId)
    return persist(app, organizationId, authorId, manifestUrl, fetched, appCredentials = null)
  }

  private fun persist(
    app: App,
    organizationId: Long,
    authorId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
    appCredentials: AppService.AppCredentials?,
  ): AppInstallService.RegisterResult {
    if (findByAppId(organizationId, fetched.manifest.id) != null) {
      throw BadRequestException(Message.APP_ALREADY_INSTALLED)
    }

    val install =
      AppInstall().apply {
        this.app = app
        this.organization = entityManager.getReference(Organization::class.java, organizationId)
        this.author = entityManager.getReference(UserAccount::class.java, authorId)
        this.principal = appInstallPrincipalService.create(fetched.manifest.name)
        this.manifestUrl = manifestUrl
        this.appId = fetched.manifest.id
        this.name = fetched.manifest.name
        this.version = fetched.manifest.version
        this.baseUrl = fetched.manifest.baseUrl
        this.manifestJson = fetched.rawJson
        this.grantedScopes = fetched.scopes.toMutableSet()
      }

    val saved =
      try {
        appInstallRepository.saveAndFlush(install)
      } catch (_: DataIntegrityViolationException) {
        throw BadRequestException(Message.APP_ALREADY_INSTALLED)
      }
    return AppInstallService.RegisterResult(
      install = saved,
      app = appService.summarize(app),
      appCredentials = appCredentials,
    )
  }

  @Transactional
  fun applySnapshot(
    organizationId: Long,
    installId: Long,
    manifestUrl: String?,
    fetched: AppManifestFetcher.FetchResult,
    allowScopeWidening: Boolean,
  ): AppInstall {
    val install =
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)

    if (fetched.manifest.id != install.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }

    manifestUrl?.let { install.manifestUrl = it }
    install.name = fetched.manifest.name
    install.version = fetched.manifest.version
    install.baseUrl = fetched.manifest.baseUrl
    install.manifestJson = fetched.rawJson
    install.grantedScopes = resolveGrantedScopes(install.grantedScopes, fetched.scopes, allowScopeWidening)

    return appInstallRepository.save(install)
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
    return appInstallRepository.findByOrganizationIdAndAppId(organizationId, appId)
  }

  private fun resolveGrantedScopes(
    current: Set<Scope>,
    fetched: Set<Scope>,
    allowScopeWidening: Boolean,
  ): MutableSet<Scope> {
    if (allowScopeWidening) return fetched.toMutableSet()
    return fetched.intersect(current).toMutableSet()
  }
}
