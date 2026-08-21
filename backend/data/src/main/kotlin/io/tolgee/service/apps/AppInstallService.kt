package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
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
) {
  data class RegisterResult(
    val install: AppInstall,
    val app: AppService.AppSummary,
    /** Non-null only when this call registered the app — see [AppService.registerIfAbsent]. */
    val appCredentials: AppService.AppCredentials?,
  )

  /**
   * Registers the app for the organization and installs it. The organization owns the app unless
   * somebody registered it first, in which case this only installs it.
   */
  fun register(
    organization: Organization,
    manifestUrl: String,
  ): RegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.registerAndCreate(organization.id, manifestUrl, fetched)
  }

  /**
   * Installs a registered app into [targetOrganization] on a server admin's behalf, bypassing the
   * availability gate that governs an organization installing an app itself — the admin is the
   * authority for a first-party enrolment. Idempotent: an organization that already has the app
   * keeps its one install.
   */
  @Transactional
  fun installForOrganizationByAdmin(
    app: App,
    targetOrganization: Organization,
  ): RegisterResult {
    appInstallRepository.findByOrganizationIdAndManifestAppId(targetOrganization.id, app.appId)?.let {
      return RegisterResult(it, AppService.AppSummary(app.id, app.appId, app.name), appCredentials = null)
    }
    val fetched = appManifestFetcher.fetch(app.manifestUrl)
    if (fetched.manifest.id != app.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }
    return appInstallPersister.create(
      appEntityId = app.id,
      organizationId = targetOrganization.id,
      fetched = fetched,
    )
  }

  /**
   * Installs an app that is already registered on this server, refusing with
   * [io.tolgee.exceptions.AppNotRegisteredException] when it is not — installing must never register
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
  ): RegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    val app = appService.requireRegistered(fetched.manifest.id)
    // An app another organization registered can only be installed once a server admin has offered
    // it to everyone; otherwise knowing its manifest URL would be enough to install it.
    if (app.organization.id != organization.id && !app.availableToAllOrganizations) {
      throw PermissionException(Message.APP_NOT_AVAILABLE_FOR_ORGANIZATION)
    }
    val registeredUrl = app.manifestUrl
    val authoritative = fetchRegistered(registeredUrl, manifestUrl, fetched)
    if (authoritative.manifest.id != app.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }
    // An install of an already-registered app discloses no credentials, so there is nothing to
    // deliver — the app reaches its new install with the app-level credentials it already holds.
    return appInstallPersister.create(
      appEntityId = app.id,
      organizationId = organization.id,
      fetched = authoritative,
    )
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

  /** The organizations that currently have the app installed. */
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
