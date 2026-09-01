package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
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
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
  private val businessEventPublisher: io.tolgee.component.reporting.BusinessEventPublisher,
) {
  data class RegisterResult(
    val install: AppInstall,
    val app: AppService.AppSummary,
    /** Non-null only when this call registered the app — see [AppService.registerIfAbsent]. */
    val appCredentials: AppService.AppCredentials?,
    /**
     * Whether the just-disclosed credentials reached the app over the lifecycle channel. Null when
     * nothing was delivered — either this call only installed an already-registered app, or it is a
     * self-registration, where the caller is the app and already holds the credentials.
     */
    val delivery: AppLifecycleDeliveryOutcome? = null,
  )

  data class SelfRegisterResult(
    val install: AppInstall,
    val created: Boolean,
    val app: AppService.AppSummary,
    val appCredentials: AppService.AppCredentials?,
  )

  /**
   * Registers the app for the organization and installs it. The organization owns the app unless
   * somebody registered it first, in which case this only installs it.
   */
  fun register(
    organization: Organization,
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    val result = registerAndCreate(organizationId = organization.id, manifestUrl = manifestUrl, author = author)
    reportInstalled(organization, result)
    return result
  }

  /**
   * Installs a registered app into [targetOrganization] on a server admin's behalf, bypassing the
   * availability gate that governs an organization installing an app itself — the admin is the
   * authority for a first-party enrolment. Idempotent: an organization that already has the app
   * keeps its one install.
   */
  @org.springframework.transaction.annotation.Transactional
  fun installForOrganizationByAdmin(
    app: io.tolgee.model.apps.App,
    targetOrganization: Organization,
    author: UserAccount,
  ): RegisterResult {
    appInstallRepository.findByOrganizationIdAndAppId(targetOrganization.id, app.appId)?.let {
      return RegisterResult(it, AppService.AppSummary(app.id, app.appId, app.name), appCredentials = null)
    }
    val fetched = appManifestFetcher.fetch(app.manifestUrl)
    if (fetched.manifest.id != app.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }
    val result =
      appInstallPersister.create(
        appEntityId = app.id,
        organizationId = targetOrganization.id,
        authorId = author.id,
        manifestUrl = app.manifestUrl,
        fetched = fetched,
      )
    reportInstalled(targetOrganization, result)
    return result
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
    author: UserAccount,
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
    val result =
      appInstallPersister.create(
        appEntityId = app.id,
        organizationId = organization.id,
        authorId = author.id,
        manifestUrl = registeredUrl,
        fetched = authoritative,
      )
    reportInstalled(organization, result)
    return result
  }

  /**
   * Reports an install to the analytics pipeline, so adoption of an app — and which organizations
   * hold it — is visible in PostHog, grouped by organization. A newly registered app reports
   * `APP_REGISTERED` too (the credentials disclosed once are what tell it apart from an install of an
   * already-registered app).
   */
  private fun reportInstalled(
    organization: Organization,
    result: RegisterResult,
  ) {
    val appData = mapOf<String, Any?>("appId" to result.app.appId, "appName" to result.app.name)
    if (result.appCredentials != null) {
      businessEventPublisher.publish(
        io.tolgee.component.reporting.OnBusinessEventToCaptureEvent(
          eventName = "APP_REGISTERED",
          organizationId = organization.id,
          organizationName = organization.name,
          data = appData,
        ),
      )
    }
    businessEventPublisher.publish(
      io.tolgee.component.reporting.OnBusinessEventToCaptureEvent(
        eventName = "APP_INSTALLED",
        organizationId = organization.id,
        organizationName = organization.name,
        data = appData + mapOf("installId" to result.install.id),
      ),
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

  private fun registerAndCreate(
    organizationId: Long,
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    val result = appInstallPersister.registerAndCreate(organizationId, author.id, manifestUrl, fetched)
    return result.copy(delivery = deliverRegistered(result))
  }

  /**
   * Hands the app its just-issued credentials, synchronously, so the registration dialog can say
   * whether the app got them or the operator still has to copy them. A failure is reported, never
   * thrown: the credentials were returned in the response too, so a dead app host does not undo the
   * registration.
   */
  private fun deliverRegistered(result: RegisterResult): AppLifecycleDeliveryOutcome? {
    val credentials = result.appCredentials ?: return null
    return appLifecycleDeliveryService.deliverNow(
      appEntityId = result.app.id,
      eventType = AppLifecycleEventType.APP_REGISTERED,
      organizationId = result.install.organization.id,
      appCredentials =
        AppLifecycleAppCredentials(
          clientId = credentials.clientId,
          clientSecret = credentials.clientSecret,
          webhookSecret = credentials.webhookSecret,
        ),
    )
  }

  /**
   * Registers an app on behalf of the server itself (no signed-in user), for the self-registration
   * flow. Re-running it for an already-registered app repoints the existing install at the new
   * manifest URL instead of failing, so an app whose dev tunnel URL changes on every restart can
   * reconnect unattended.
   *
   * The app-level credentials are disclosed only when this call registered the app; a repoint
   * discloses nothing, or anyone holding the registration secret could silently mint fresh
   * credentials for an existing app.
   *
   * @param organization the organization the app registers into and that owns it.
   */
  fun selfRegister(
    organization: Organization,
    manifestUrl: String,
    author: UserAccount,
  ): SelfRegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    val organizationId = organization.id
    val existing = appInstallRepository.findByOrganizationIdAndAppId(organizationId, fetched.manifest.id)

    if (existing == null) {
      // No delivery on this path: the caller is the app itself and reads the credentials straight
      // out of this call's response.
      val created = appInstallPersister.registerAndCreate(organizationId, author.id, manifestUrl, fetched)
      reportInstalled(organization, created)
      return SelfRegisterResult(
        install = created.install,
        created = true,
        app = created.app,
        appCredentials = created.appCredentials,
      )
    }

    val updated =
      appInstallPersister.applySnapshot(
        organizationId = organizationId,
        installId = existing.id,
        manifestUrl = manifestUrl,
        fetched = fetched,
        allowScopeWidening = true,
      )
    return SelfRegisterResult(
      install = updated,
      created = false,
      app = appService.summarize(appService.requireRegistered(fetched.manifest.id)),
      appCredentials = null,
    )
  }

  fun previewManifest(manifestUrl: String): AppManifestFetcher.FetchResult {
    return appManifestFetcher.fetch(manifestUrl)
  }

  fun refresh(
    organizationId: Long,
    installId: Long,
  ): AppInstall {
    val manifestUrl = requireInstall(organizationId, installId).manifestUrl
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.applySnapshot(
      organizationId = organizationId,
      installId = installId,
      manifestUrl = null,
      fetched = fetched,
      allowScopeWidening = true,
    )
  }

  /**
   * @param allowScopeWidening whether the re-fetched manifest may grant scopes beyond those
   *   already consented to. True only for owner-initiated calls (the org owner is the consent
   *   authority). Must be false for app-initiated calls: an app could otherwise self-grant
   *   arbitrary scopes by repointing at a manifest declaring more.
   */
  fun updateManifestUrl(
    organizationId: Long,
    installId: Long,
    manifestUrl: String,
    allowScopeWidening: Boolean,
  ): AppInstall {
    requireInstall(organizationId, installId)
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.applySnapshot(
      organizationId = organizationId,
      installId = installId,
      manifestUrl = manifestUrl,
      fetched = fetched,
      allowScopeWidening = allowScopeWidening,
    )
  }

  /**
   * Uninstalls the app from one organization. The app itself stays registered and every other
   * organization's install is untouched — removing the app everywhere is the owner's operation, see
   * [AppOwnerRemovalService].
   *
   */
  fun remove(
    organizationId: Long,
    installId: Long,
  ) {
    // No delivery: an uninstall carries no secret, and an app that tracks its installs sees this one
    // vanish from its own discovery call.
    appInstallPersister.remove(organizationId, installId)
  }

  @Transactional(readOnly = true)
  fun findAll(organizationId: Long): List<AppInstall> {
    return appInstallRepository.findAllByOrganizationId(organizationId)
  }

  /** The organizations that currently have the app installed. */
  @Transactional(readOnly = true)
  fun findInstallingOrganizations(appEntityId: Long): List<Organization> {
    return appInstallRepository.findInstallingOrganizations(appEntityId)
  }

  @Transactional(readOnly = true)
  fun find(
    organizationId: Long,
    installId: Long,
  ): AppInstall? {
    return appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
  }

  private fun requireInstall(
    organizationId: Long,
    installId: Long,
  ): AppInstall {
    return appInstallRepository.findByOrganizationIdAndId(organizationId, installId)
      ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
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
   * Resolves an install together with the identity an install-context request runs as, for the
   * app-token auth filter.
   *
   * That identity is the install's own [AppInstall.principal], never the person who registered it:
   * an install belongs to its organization and must keep working after that person is disabled or
   * deleted, and nothing of theirs — server role, organization membership, project permissions,
   * per-language grants — may reach the install through the principal. The principal holds none of
   * those, so everything the install may do comes from [AppInstall.grantedScopes] — see
   * [io.tolgee.service.security.SecurityService.getCurrentPermittedScopes].
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
