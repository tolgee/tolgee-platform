package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Orchestrates without a transaction: the manifest fetch reaches an app-controlled host that may
 * stall for seconds, and holding a pooled DB connection across it would let a handful of slow hosts
 * exhaust the pool. Writes are delegated to [AppInstallPersister].
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
    /** Non-null only when this call registered the app - see [AppService.registerIfAbsent]. */
    val appCredentials: AppService.AppCredentials?,
  )

  data class RegisterAppResult(
    val app: AppService.AppSummary,
    val appEntityId: Long,
    /** Non-null only when this call registered the app. */
    val appCredentials: AppService.AppCredentials?,
    /** Non-null when the app was also installed for the owner (the default). */
    val install: AppInstall?,
  )

  /**
   * Registers the app for the organization and, unless [install] is false, installs it. The
   * organization owns the app unless somebody registered it first, in which case this only installs
   * it (when [install]) and returns no credentials.
   */
  fun register(
    organization: Organization,
    manifestUrl: String,
    manifestHash: String?,
    install: Boolean,
  ): RegisterAppResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    verifyManifestUnchanged(manifestHash, fetched)
    return appInstallPersister.registerAndMaybeInstall(organization.id, manifestUrl, fetched, install)
  }

  /**
   * Installs an app that is already registered on this server, refusing with
   * [io.tolgee.exceptions.AppNotRegisteredException] when it is not - installing must never register
   * an app behind the caller's back, because registering hands out app-level credentials and makes
   * the caller's organization the app's owner.
   *
   * The install snapshot is taken from the **registered** manifest URL, not from the one the caller
   * pasted: an app is identified by the id in its manifest, so a lookalike manifest served elsewhere
   * would otherwise decide the scopes and base URL of an install of somebody else's app.
   */
  fun install(
    organization: Organization,
    manifestUrl: String,
    manifestHash: String?,
  ): AppInstall {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    verifyManifestUnchanged(manifestHash, fetched)
    val app = appService.requireRegistered(fetched.manifest.id)
    if (!appAvailabilityService.isAvailableForOrganization(app.organization.id, app.id, organization.id)) {
      throw PermissionException(Message.APP_NOT_AVAILABLE_FOR_ORGANIZATION)
    }
    val authoritative = fetchRegistered(app.manifestUrl, manifestUrl, fetched)
    if (authoritative.manifest.id != app.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }
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

  /** The SHA-256 hex of the manifest as fetched, so the consent preview and the write agree on it. */
  fun manifestHash(fetched: AppManifestFetcher.FetchResult): String {
    return DigestUtils.sha256Hex(fetched.rawJson)
  }

  /**
   * Rejects a manifest whose bytes changed between the consent preview and this write, so an app
   * cannot widen the scopes it requests after they were approved. Skipped when the caller sends no
   * hash (nothing was previewed to compare against).
   */
  private fun verifyManifestUnchanged(
    expectedHash: String?,
    fetched: AppManifestFetcher.FetchResult,
  ) {
    if (expectedHash.isNullOrBlank()) return
    if (manifestHash(fetched) != expectedHash) {
      throw BadRequestException(Message.APP_MANIFEST_CHANGED)
    }
  }

  /**
   * Uninstalls the app from one organization. The app itself stays registered and every other
   * organization's install is untouched.
   */
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

  /** The organizations that currently have the app installed, for the admin installations view. */
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
}
