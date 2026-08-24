package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.dao.DataIntegrityViolationException
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
    /** Always null here: installing an already-registered app discloses no app-level credentials. */
    val appCredentials: AppService.AppCredentials?,
  )

  data class RegisterAppResult(
    val app: AppService.AppSummary,
    val appEntityId: Long,
    /** The app-level credentials, disclosed only in this response to registering the app. */
    val appCredentials: AppService.AppCredentials?,
    /** Non-null when the app was also installed for the owner (the default). */
    val install: AppInstall?,
  )

  /**
   * Registers the app for the organization - making it the app's owner - and, unless [install] is
   * false, installs it, all in one transaction. Registering an app that is already registered is
   * refused with [Message.APP_ALREADY_REGISTERED]; installing an existing app is the separate
   * install endpoint. This runs outside a transaction so the concurrent-duplicate race can be caught
   * after the write's own transaction has fully rolled back.
   */
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

  /**
   * Installs an app that is already registered on this server, refusing with
   * [io.tolgee.exceptions.AppNotRegisteredException] when it is not - installing must never register
   * an app behind the caller's back, because registering hands out app-level credentials and makes
   * the caller's organization the app's owner.
   *
   * The install snapshot is taken from the **registered** manifest URL, not from the one the caller
   * pasted: an app is identified by the id in its manifest, so a lookalike manifest served elsewhere
   * would otherwise decide the scopes and base URL of an install of somebody else's app. The consent
   * hash is likewise checked against that authoritative manifest - the one actually installed - not
   * against the pasted fetch.
   */
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

  /**
   * Resolves an install by id alone, for the app-token auth filter. Tenant safety on this path comes
   * from the enablement re-check (the token is bound to a project the app is enabled for), not from
   * an org-scoped lookup — the app token legitimately acts across the projects it is enabled in.
   */
  @Transactional(readOnly = true)
  fun findForAppAuth(installId: Long): AppInstall? {
    return appInstallRepository.findWithAppById(installId)
  }

  /**
   * Resolves an install together with the identity an install-context request runs as — the
   * install's own [AppInstall.principal] (see its definition for why, and for where the install's
   * capability comes from instead).
   */
  @Transactional(readOnly = true)
  fun resolveForAppAuth(installId: Long): AppAuthResolution? {
    val install = appInstallRepository.findWithAppById(installId) ?: return null
    return AppAuthResolution(install, UserAccountDto.fromEntity(install.principal))
  }

  data class AppAuthResolution(
    val install: AppInstall,
    /** The install acting as itself. */
    val principal: UserAccountDto,
  )

  /** Every installation of the app, across organizations, for the app's own discovery call. */
  @Transactional(readOnly = true)
  fun findAllByRegisteredApp(appEntityId: Long): List<AppInstall> {
    return appInstallRepository.findAllByRegisteredAppId(appEntityId)
  }

  /**
   * The app's own install [installId], for an app minting a token with its app-level credentials.
   *
   * Returns null both when no such install exists and when it belongs to a different app, so an
   * authenticated app cannot use this to learn which install ids exist outside its own.
   */
  @Transactional(readOnly = true)
  fun findOwnInstall(
    appEntityId: Long,
    installId: Long,
  ): AppInstall? {
    val install = appInstallRepository.findWithAppById(installId) ?: return null
    if (install.app.id != appEntityId) return null
    return install
  }
}
