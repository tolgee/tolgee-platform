package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppInstallService(
  private val appInstallRepository: AppInstallRepository,
  private val appManifestFetcher: AppManifestFetcher,
  private val appEnablementService: AppEnablementService,
  private val keyGenerator: KeyGenerator,
  private val appManagedAutomationService: AppManagedAutomationService,
) {
  data class RegisterResult(
    val install: AppInstall,
    val plaintextClientSecret: String,
  )

  data class AppCredentialResolution(
    val install: AppInstall,
    val authorPrincipal: UserAccountDto,
  )

  @Transactional
  fun register(
    organization: Organization,
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)

    if (appInstallRepository.findByOrganizationIdAndAppId(organization.id, fetched.manifest.id) != null) {
      throw BadRequestException(Message.APP_ALREADY_INSTALLED)
    }

    val plaintextClientId = CLIENT_ID_PREFIX + keyGenerator.generate(128)
    val plaintextClientSecret = CLIENT_SECRET_PREFIX + keyGenerator.generate(256)
    val plaintextWebhookSecret = WEBHOOK_SECRET_PREFIX + keyGenerator.generate(256)

    val install =
      AppInstall().apply {
        this.organization = organization
        this.author = author
        this.manifestUrl = manifestUrl
        this.appId = fetched.manifest.id
        this.name = fetched.manifest.name
        this.version = fetched.manifest.version
        this.baseUrl = fetched.manifest.baseUrl
        this.manifestJson = fetched.rawJson
        this.grantedScopes = fetched.scopes.toMutableSet()
        this.webhookSubscriptions = fetched.webhookEvents.toMutableSet()
        this.webhookUrl = fetched.resolvedWebhookUrl
        this.clientId = plaintextClientId
        this.clientSecretHash = keyGenerator.hash(plaintextClientSecret)
        this.clientSecretPrefix = plaintextClientSecret.take(CLIENT_SECRET_PREFIX_DISPLAY_LENGTH)
        this.webhookSecret = plaintextWebhookSecret
      }

    val saved = appInstallRepository.save(install)
    return RegisterResult(install = saved, plaintextClientSecret = plaintextClientSecret)
  }

  @Transactional(readOnly = true)
  fun findAll(organizationId: Long): List<AppInstall> {
    return appInstallRepository.findAllByOrganizationId(organizationId)
  }

  @Transactional(readOnly = true)
  fun find(installId: Long): AppInstall? {
    return appInstallRepository.findById(installId).orElse(null)
  }

  @Transactional(readOnly = true)
  fun findByClientSecretHash(clientSecretHash: String): AppInstall? {
    return appInstallRepository.findByClientSecretHash(clientSecretHash)
  }

  @Transactional(readOnly = true)
  fun findByClientId(clientId: String): AppInstall? {
    return appInstallRepository.findByClientId(clientId)
  }

  @Transactional(readOnly = true)
  fun resolveByClientSecretHash(clientSecretHash: String): AppCredentialResolution? {
    val install = appInstallRepository.findByClientSecretHash(clientSecretHash) ?: return null
    return AppCredentialResolution(install, UserAccountDto.fromEntity(install.author))
  }

  @Transactional(readOnly = true)
  fun resolveByClientId(clientId: String): AppCredentialResolution? {
    val install = appInstallRepository.findByClientId(clientId) ?: return null
    return AppCredentialResolution(install, UserAccountDto.fromEntity(install.author))
  }

  fun previewManifest(manifestUrl: String): AppManifestFetcher.FetchResult {
    return appManifestFetcher.fetch(manifestUrl)
  }

  @Transactional
  fun refresh(
    organizationId: Long,
    installId: Long,
  ): AppInstall {
    val install =
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)

    val fetched = appManifestFetcher.fetch(install.manifestUrl)

    if (fetched.manifest.id != install.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }

    install.name = fetched.manifest.name
    install.version = fetched.manifest.version
    install.baseUrl = fetched.manifest.baseUrl
    install.manifestJson = fetched.rawJson
    install.grantedScopes = fetched.scopes.toMutableSet()
    install.webhookSubscriptions = fetched.webhookEvents.toMutableSet()
    install.webhookUrl = fetched.resolvedWebhookUrl

    val saved = appInstallRepository.save(install)
    appManagedAutomationService.onInstallRefresh(saved)
    return saved
  }

  @Transactional
  fun updateManifestUrl(
    organizationId: Long,
    installId: Long,
    manifestUrl: String,
  ): AppInstall {
    val install =
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)

    val fetched = appManifestFetcher.fetch(manifestUrl)

    if (fetched.manifest.id != install.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }

    install.manifestUrl = manifestUrl
    install.name = fetched.manifest.name
    install.version = fetched.manifest.version
    install.baseUrl = fetched.manifest.baseUrl
    install.manifestJson = fetched.rawJson
    install.grantedScopes = fetched.scopes.toMutableSet()
    install.webhookSubscriptions = fetched.webhookEvents.toMutableSet()
    install.webhookUrl = fetched.resolvedWebhookUrl

    val saved = appInstallRepository.save(install)
    appManagedAutomationService.onInstallRefresh(saved)
    return saved
  }

  @Transactional
  fun remove(
    organizationId: Long,
    installId: Long,
  ) {
    val install =
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    appEnablementService.removeAllForAppInstall(installId)
    appInstallRepository.delete(install)
  }

  companion object {
    const val CLIENT_ID_PREFIX = "tgapp_"
    const val CLIENT_SECRET_PREFIX = "tgapps_"
    const val WEBHOOK_SECRET_PREFIX = "tgappw_"
    const val CLIENT_SECRET_PREFIX_DISPLAY_LENGTH = 10
  }
}
