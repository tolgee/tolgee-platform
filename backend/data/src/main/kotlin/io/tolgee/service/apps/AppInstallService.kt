package io.tolgee.service.apps

import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.dtos.cacheable.AppDto
import io.tolgee.dtos.cacheable.AppInstallDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.service.security.UserAccountService
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
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
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
  private val userAccountService: UserAccountService,
  @Lazy
  private val self: AppInstallService,
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
    /**
     * Whether the just-disclosed credentials reached the app over the lifecycle channel. Null when
     * nothing was disclosed (there is no such flow yet — registration always discloses).
     */
    val delivery: AppLifecycleDeliveryOutcome? = null,
  )

  fun register(
    organization: Organization,
    manifestUrl: String,
    manifestHash: String?,
    install: Boolean,
  ): RegisterAppResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    verifyManifestUnchanged(manifestHash, fetched)
    val result =
      try {
        appInstallPersister.registerAndMaybeInstall(organization.id, manifestUrl, fetched, install)
      } catch (_: DataIntegrityViolationException) {
        throw BadRequestException(Message.APP_ALREADY_REGISTERED)
      }
    return result.copy(delivery = deliverRegistered(organization, result))
  }

  /**
   * Hands the app its just-issued credentials, synchronously, so the registration dialog can say
   * whether the app got them or the operator still has to copy them. A failure is reported, never
   * thrown: the credentials were returned in the response too, so a dead app host does not undo the
   * registration. Runs after [registerAndMaybeInstall]'s transaction commits.
   */
  private fun deliverRegistered(
    organization: Organization,
    result: RegisterAppResult,
  ): AppLifecycleDeliveryOutcome? {
    val credentials = result.appCredentials ?: return null
    return appLifecycleDeliveryService.deliverNow(
      appEntityId = result.appEntityId,
      eventType = AppLifecycleEventType.APP_REGISTERED,
      organizationId = organization.id,
      appCredentials =
        AppLifecycleAppCredentials(
          clientId = credentials.clientId,
          clientSecret = credentials.clientSecret,
          webhookSecret = credentials.webhookSecret,
        ),
    )
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

  /**
   * Re-fetches the manifest for an organization's install and reconciles the snapshot. The caller is
   * the organization's app manager and the consent authority, so [allowScopeWidening] is true: the
   * install adopts the scope set the manifest currently requests, which is how a widened permission
   * request is approved. The fetch runs outside a transaction — see the class doc.
   */
  fun refresh(
    organizationId: Long,
    installId: Long,
  ): AppInstall {
    val manifestUrl = requireOrgInstallManifestUrl(organizationId, installId)
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.applySnapshotForOrgInstall(
      organizationId = organizationId,
      installId = installId,
      fetched = fetched,
      allowScopeWidening = true,
    )
  }

  /**
   * Re-fetches the manifest for an install on the calling app's own behalf. Never widens the granted
   * scopes: a manifest that requests more surfaces those as pending until the organization's owner
   * approves them through [refresh]. Refuses when the install is not the app's own.
   */
  fun refreshForApp(
    appEntityId: Long,
    installId: Long,
  ): AppInstall {
    val manifestUrl =
      findOwnInstall(appEntityId, installId)?.app?.manifestUrl
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.applySnapshotForApp(appEntityId, installId, fetched)
  }

  @Transactional(readOnly = true)
  fun requireOrgInstallManifestUrl(
    organizationId: Long,
    installId: Long,
  ): String {
    return (
      appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    ).app.manifestUrl
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

  @Cacheable(cacheNames = [Caches.APP_INSTALLS], key = "#installId")
  @Transactional(readOnly = true)
  fun findForAppAuth(installId: Long): AppInstallDto? {
    return appInstallRepository.findWithAppById(installId)?.let { AppInstallDto.fromEntity(it) }
  }

  fun resolveForAppAuth(installId: Long): AppAuthResolution? {
    val install = self.findForAppAuth(installId) ?: return null
    val principal = userAccountService.findDto(install.principalUserId) ?: return null
    return AppAuthResolution(install, principal)
  }

  data class AppAuthResolution(
    val install: AppInstallDto,
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

  @Cacheable(cacheNames = [Caches.APPS], key = "#appEntityId")
  @Transactional(readOnly = true)
  fun findAppForAppAuth(appEntityId: Long): AppDto? {
    return appService.findForAppAuth(appEntityId)?.let { AppDto.fromEntity(it) }
  }
}
