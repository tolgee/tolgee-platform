package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.AppInstall
import io.tolgee.repository.apps.AppInstallRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    val plaintextClientSecret: String,
    val app: AppService.AppSummary,
    /** Non-null only when this call registered the app — see [AppService.registerIfAbsent]. */
    val appCredentials: AppService.AppCredentials?,
  )

  data class SelfRegisterResult(
    val install: AppInstall,
    /** Non-null only when this call created the install; see [selfRegister]. */
    val plaintextClientSecret: String?,
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
    return registerAndCreate(organizationId = organization.id, manifestUrl = manifestUrl, author = author)
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
    val registeredUrl = app.manifestUrl
    val authoritative = fetchRegistered(registeredUrl, manifestUrl, fetched)
    if (authoritative.manifest.id != app.appId) {
      throw BadRequestException(Message.APP_MANIFEST_INVALID)
    }
    return appInstallPersister.create(
      appEntityId = app.id,
      organizationId = organization.id,
      authorId = author.id,
      manifestUrl = registeredUrl,
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

  /**
   * Registers a native (server-level) install owned by no organization, on behalf of a server admin.
   * Unlike [selfRegister] it never repoints an existing install — an admin pasting a manifest whose
   * app is already registered gets the same "already installed" error as an organization owner does.
   */
  fun registerNative(
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    return registerAndCreate(organizationId = null, manifestUrl = manifestUrl, author = author)
  }

  private fun registerAndCreate(
    organizationId: Long?,
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.registerAndCreate(organizationId, author.id, manifestUrl, fetched)
  }

  /**
   * Registers an app on behalf of the server itself (no signed-in user), for the self-registration
   * flow. Re-running it for an already-registered app repoints the existing install at the new
   * manifest URL instead of failing, so an app whose dev tunnel URL changes on every restart can
   * reconnect unattended.
   *
   * The returned [SelfRegisterResult.plaintextClientSecret] is null on that repoint path: the secret
   * is only ever disclosed at creation, and re-issuing it here would let anyone holding the
   * registration secret silently mint fresh credentials for an existing install.
   *
   * @param organization null registers a native (server-level) install; a server admin then decides
   *   which organizations may use it.
   */
  fun selfRegister(
    organization: Organization?,
    manifestUrl: String,
    author: UserAccount,
  ): SelfRegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    val organizationId = organization?.id
    val existing = findForSelfRegister(organizationId, fetched.manifest.id)

    if (existing == null) {
      val created = appInstallPersister.registerAndCreate(organizationId, author.id, manifestUrl, fetched)
      return SelfRegisterResult(
        install = created.install,
        plaintextClientSecret = created.plaintextClientSecret,
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
      plaintextClientSecret = null,
      created = false,
      app = appService.summarize(appService.requireRegistered(fetched.manifest.id)),
      appCredentials = null,
    )
  }

  private fun findForSelfRegister(
    organizationId: Long?,
    appId: String,
  ): AppInstall? {
    if (organizationId == null) return appInstallRepository.findByOrganizationIsNullAndAppId(appId)
    return appInstallRepository.findByOrganizationIdAndAppId(organizationId, appId)
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

  /** @param organizationId null targets a native (server-level) install. */
  fun remove(
    organizationId: Long?,
    installId: Long,
  ) {
    appInstallPersister.remove(organizationId, installId)
  }

  @Transactional(readOnly = true)
  fun findAll(organizationId: Long): List<AppInstall> {
    return appInstallRepository.findAllByOrganizationId(organizationId)
  }

  @Transactional(readOnly = true)
  fun findAllNativePaged(pageable: Pageable): Page<AppInstall> {
    return appInstallRepository.findAllByOrganizationIsNull(pageable)
  }

  @Transactional(readOnly = true)
  fun isNative(installId: Long): Boolean {
    return appInstallRepository.findByOrganizationIsNullAndId(installId) != null
  }

  @Transactional(readOnly = true)
  fun getNative(installId: Long): AppInstall {
    return appInstallRepository.findByOrganizationIsNullAndId(installId)
      ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
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
    return appInstallRepository.findById(installId).orElse(null)
  }

  /**
   * Resolves an install together with the identity an install-context request is recorded under, for
   * the app-token auth filter.
   *
   * The author is looked up **without** the active-user filter and with [UserAccountDto.role]
   * cleared: an install belongs to its organization, not to the person who created it, so the app
   * must keep working after that person is disabled or deleted, and no server role may reach the
   * install through them. Everything the install may actually do comes from
   * [AppInstall.grantedScopes] — see
   * [io.tolgee.service.security.SecurityService.getCurrentPermittedScopes].
   */
  @Transactional(readOnly = true)
  fun resolveForAppAuth(installId: Long): AppAuthResolution? {
    val install = appInstallRepository.findById(installId).orElse(null) ?: return null
    return AppAuthResolution(install, UserAccountDto.fromEntity(install.author).copy(role = null))
  }

  data class AppAuthResolution(
    val install: AppInstall,
    /** Who created the install. Identity and audit only; it grants nothing. */
    val author: UserAccountDto,
  )

  /**
   * Resolves an install by its OAuth `client_id`, for the token endpoint. The caller must still
   * verify the presented secret against the install's live [io.tolgee.model.apps.AppInstallSecret]s.
   */
  @Transactional(readOnly = true)
  fun resolveByClientId(clientId: String): AppInstall? {
    return appInstallRepository.findByClientId(clientId)
  }

  /** @param organizationId null targets a native (server-level) install. */
  @Transactional(readOnly = true)
  fun getScoped(
    organizationId: Long?,
    installId: Long,
  ): AppInstall {
    if (organizationId == null) return getNative(installId)
    return requireInstall(organizationId, installId)
  }

  companion object {
    const val CLIENT_ID_PREFIX = "tgapp_"
    const val CLIENT_SECRET_PREFIX = "tgapps_"
    const val CLIENT_SECRET_PREFIX_DISPLAY_LENGTH = 10
  }
}
