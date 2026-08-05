package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
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
) {
  data class RegisterResult(
    val install: AppInstall,
    val plaintextClientSecret: String,
  )

  data class SelfRegisterResult(
    val install: AppInstall,
    /** Non-null only when this call created the install; see [selfRegister]. */
    val plaintextClientSecret: String?,
    val created: Boolean,
  )

  data class AppCredentialResolution(
    val install: AppInstall,
    val authorPrincipal: UserAccountDto,
  )

  fun register(
    organization: Organization,
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    return create(organizationId = organization.id, manifestUrl = manifestUrl, author = author)
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
    return create(organizationId = null, manifestUrl = manifestUrl, author = author)
  }

  private fun create(
    organizationId: Long?,
    manifestUrl: String,
    author: UserAccount,
  ): RegisterResult {
    val fetched = appManifestFetcher.fetch(manifestUrl)
    return appInstallPersister.create(organizationId, author.id, manifestUrl, fetched)
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
      val created = appInstallPersister.create(organizationId, author.id, manifestUrl, fetched)
      return SelfRegisterResult(
        install = created.install,
        plaintextClientSecret = created.plaintextClientSecret,
        created = true,
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
    return SelfRegisterResult(install = updated, plaintextClientSecret = null, created = false)
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
   * Resolves an install by id alone plus the principal (the install author) that an install-context
   * token acts as. Used by the app-token auth filter for the machine-to-machine (OAuth
   * client-credentials) path.
   */
  @Transactional(readOnly = true)
  fun resolveForAppAuth(installId: Long): AppCredentialResolution? {
    val install = appInstallRepository.findById(installId).orElse(null) ?: return null
    return AppCredentialResolution(install, UserAccountDto.fromEntity(install.author))
  }

  /**
   * Resolves an install by its OAuth `client_id`, for the token endpoint. The caller must still
   * verify the presented client secret against [AppInstall.clientSecretHash].
   */
  @Transactional(readOnly = true)
  fun resolveByClientId(clientId: String): AppCredentialResolution? {
    val install = appInstallRepository.findByClientId(clientId) ?: return null
    return AppCredentialResolution(install, UserAccountDto.fromEntity(install.author))
  }

  companion object {
    const val CLIENT_ID_PREFIX = "tgapp_"
    const val CLIENT_SECRET_PREFIX = "tgapps_"
    const val CLIENT_SECRET_PREFIX_DISPLAY_LENGTH = 10
  }
}
