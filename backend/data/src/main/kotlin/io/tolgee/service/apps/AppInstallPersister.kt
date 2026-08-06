package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
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
  private val appAvailabilityService: AppAvailabilityService,
  private val appInstallSecretService: AppInstallSecretService,
  private val entityManager: EntityManager,
  private val keyGenerator: KeyGenerator,
) {
  /** @param organizationId null registers a native (server-level) install owned by no organization. */
  @Transactional
  fun create(
    organizationId: Long?,
    authorId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
  ): AppInstallService.RegisterResult {
    if (findByAppId(organizationId, fetched.manifest.id) != null) {
      throw BadRequestException(Message.APP_ALREADY_INSTALLED)
    }

    val plaintextClientId = AppInstallService.CLIENT_ID_PREFIX + keyGenerator.generate(128)

    val install =
      AppInstall().apply {
        this.organization = organizationId?.let { entityManager.getReference(Organization::class.java, it) }
        this.author = entityManager.getReference(UserAccount::class.java, authorId)
        this.manifestUrl = manifestUrl
        this.appId = fetched.manifest.id
        this.name = fetched.manifest.name
        this.version = fetched.manifest.version
        this.baseUrl = fetched.manifest.baseUrl
        this.manifestJson = fetched.rawJson
        this.grantedScopes = fetched.scopes.toMutableSet()
        this.clientId = plaintextClientId
      }

    val saved =
      try {
        appInstallRepository.saveAndFlush(install)
      } catch (_: DataIntegrityViolationException) {
        throw BadRequestException(Message.APP_ALREADY_INSTALLED)
      }
    val issued = appInstallSecretService.issueInitial(saved)
    return AppInstallService.RegisterResult(install = saved, plaintextClientSecret = issued.plaintextSecret)
  }

  @Transactional
  fun applySnapshot(
    organizationId: Long?,
    installId: Long,
    manifestUrl: String?,
    fetched: AppManifestFetcher.FetchResult,
    allowScopeWidening: Boolean,
  ): AppInstall {
    val install =
      findScopedInstall(organizationId, installId)
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
    organizationId: Long?,
    installId: Long,
  ) {
    val install =
      findScopedInstall(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    appEnablementService.removeAllForAppInstall(installId)
    appAvailabilityService.removeAllForAppInstall(installId)
    appInstallRepository.delete(install)
  }

  private fun findScopedInstall(
    organizationId: Long?,
    installId: Long,
  ): AppInstall? {
    if (organizationId == null) return appInstallRepository.findByOrganizationIsNullAndId(installId)
    return appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
  }

  private fun findByAppId(
    organizationId: Long?,
    appId: String,
  ): AppInstall? {
    if (organizationId == null) return appInstallRepository.findByOrganizationIsNullAndAppId(appId)
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
