package io.tolgee.service.apps

import io.tolgee.constants.Message
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
) {
  @Transactional
  fun register(
    organization: Organization,
    manifestUrl: String,
    author: UserAccount,
  ): AppInstall {
    val fetched = appManifestFetcher.fetch(manifestUrl)

    if (appInstallRepository.findByOrganizationIdAndAppId(organization.id, fetched.manifest.id) != null) {
      throw BadRequestException(Message.APP_ALREADY_INSTALLED)
    }

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
      }

    return appInstallRepository.save(install)
  }

  @Transactional(readOnly = true)
  fun findAll(organizationId: Long): List<AppInstall> {
    return appInstallRepository.findAllByOrganizationId(organizationId)
  }

  @Transactional(readOnly = true)
  fun find(installId: Long): AppInstall? {
    return appInstallRepository.findById(installId).orElse(null)
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
    appEnablementService.removeAllForAppInstall(installId)
    appInstallRepository.delete(install)
  }
}
