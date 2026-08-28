package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Orchestrates without a transaction: the manifest fetch reaches an app-controlled host that may
 * stall for seconds, and holding a pooled DB connection across it would let a handful of slow hosts
 * exhaust the pool.
 */
@Service
class AppInstallService(
  private val appInstallRepository: AppInstallRepository,
  private val appManifestFetcher: AppManifestFetcher,
  private val appInstallPersister: AppInstallPersister,
  private val appService: AppService,
  private val appAvailabilityService: AppAvailabilityService,
) {
  data class RegisterResult(
    val install: AppInstall,
    val app: AppService.AppSummary,
    val appCredentials: AppService.AppCredentials?,
  )

  data class RegisterAppResult(
    val app: AppService.AppSummary,
    val appEntityId: Long,
    val appCredentials: AppService.AppCredentials?,
    val install: AppInstall?,
  )

  fun register(
    organization: Organization,
    manifestUrl: String,
    manifestHash: String?,
    install: Boolean,
  ): RegisterAppResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    verifyManifestUnchanged(manifestHash, fetched)
    return try {
      appInstallPersister.registerAndMaybeInstall(organization.id, manifestUrl, fetched, install)
    } catch (_: DataIntegrityViolationException) {
      throw BadRequestException(Message.APP_ALREADY_REGISTERED)
    }
  }

  fun install(
    organization: Organization,
    manifestUrl: String,
    manifestHash: String?,
  ): AppInstall {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    val app = appService.requireRegistered(fetched.manifest.id)
    if (!appAvailabilityService.isAvailableForOrganization(app.organization.id, app.id, organization.id)) {
      throw PermissionException(Message.APP_NOT_AVAILABLE_FOR_ORGANIZATION)
    }
    val authoritative = fetchRegistered(app.manifestUrl, manifestUrl, fetched)
    if (authoritative.manifest.id != app.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }
    verifyManifestUnchanged(manifestHash, authoritative)
    return appInstallPersister
      .create(appEntityId = app.id, organizationId = organization.id, fetched = authoritative)
      .install
  }

  private fun fetchRegistered(
    registeredUrl: String,
    requestedUrl: String,
    alreadyFetched: AppManifestFetcher.FetchResult,
  ): AppManifestFetcher.FetchResult {
    if (registeredUrl == requestedUrl) return alreadyFetched
    return appManifestFetcher.fetch(registeredUrl)
  }

  fun previewManifest(manifestUrl: String): AppManifestFetcher.FetchResult {
    return appManifestFetcher.fetch(manifestUrl)
  }

  fun manifestHash(fetched: AppManifestFetcher.FetchResult): String {
    return DigestUtils.sha256Hex(fetched.rawJson)
  }

  private fun verifyManifestUnchanged(
    expectedHash: String?,
    fetched: AppManifestFetcher.FetchResult,
  ) {
    if (expectedHash.isNullOrBlank()) return
    if (manifestHash(fetched) != expectedHash) {
      throw BadRequestException(Message.APP_MANIFEST_CHANGED)
    }
  }

  fun remove(
    organizationId: Long,
    installId: Long,
  ) {
    appInstallPersister.remove(organizationId, installId)
  }

  @Transactional(readOnly = true)
  fun findAll(organizationId: Long): List<AppInstall> {
    return appInstallRepository.findAllByOrganizationId(organizationId)
  }

  @Transactional(readOnly = true)
  fun findInstallingOrganizations(
    appEntityId: Long,
    search: String?,
    pageable: org.springframework.data.domain.Pageable,
  ): org.springframework.data.domain.Page<Organization> {
    return appInstallRepository.findInstallingOrganizations(appEntityId, search?.ifBlank { null }, pageable)
  }

  @Transactional(readOnly = true)
  fun find(
    organizationId: Long,
    installId: Long,
  ): AppInstall? {
    return appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
  }

  @Transactional(readOnly = true)
  fun findForAppAuth(installId: Long): AppInstall? {
    return appInstallRepository.findWithAppById(installId)
  }

  @Transactional(readOnly = true)
  fun resolveForAppAuth(installId: Long): AppAuthResolution? {
    val install = appInstallRepository.findWithAppById(installId) ?: return null
    return AppAuthResolution(install, UserAccountDto.fromEntity(install.principal))
  }

  data class AppAuthResolution(
    val install: AppInstall,
    val principal: UserAccountDto,
  )

  @Transactional(readOnly = true)
  fun findAllByRegisteredApp(appEntityId: Long): List<AppInstall> {
    return appInstallRepository.findAllByRegisteredAppId(appEntityId)
  }

  @Transactional(readOnly = true)
  fun findOwnInstall(
    appEntityId: Long,
    installId: Long,
  ): AppInstall? {
    val install = appInstallRepository.findWithAppById(installId) ?: return null
    if (install.app.id != appEntityId) return null
    return install
  }

  @Transactional(readOnly = true)
  fun findAppForAppAuth(appEntityId: Long): App? {
    return appService.findForAppAuth(appEntityId)
  }
}
