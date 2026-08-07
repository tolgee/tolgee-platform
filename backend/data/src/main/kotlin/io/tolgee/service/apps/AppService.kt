package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.exceptions.AppNotRegisteredException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.apps.App
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.repository.apps.AppRepository
import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppService(
  private val appRepository: AppRepository,
  private val appInstallRepository: AppInstallRepository,
  private val appSecretService: AppSecretService,
  private val keyGenerator: KeyGenerator,
  private val entityManager: EntityManager,
) {
  /** The app-level credentials, disclosed only in the response to registering the app. */
  data class AppCredentials(
    val clientId: String,
    val clientSecret: String,
    val webhookSecret: String,
  )

  data class AppSummary(
    val id: Long,
    val appId: String,
    val name: String,
  )

  data class ResolveResult(
    val app: App,
    /** Non-null only when this call registered the app. */
    val credentials: AppCredentials?,
  )

  @Transactional(readOnly = true)
  fun find(appId: String): App? {
    return appRepository.findByAppId(appId)
  }

  @Transactional(readOnly = true)
  fun requireRegistered(appId: String): App {
    return appRepository.findByAppId(appId) ?: throw AppNotRegisteredException(appId)
  }

  /**
   * The app, provided [organizationId] owns it. An organization that merely installed somebody
   * else's app must not reach it here — administering an app is the owner's alone.
   */
  @Transactional(readOnly = true)
  fun getOwned(
    organizationId: Long,
    appEntityId: Long,
  ): App {
    return appRepository.findByIdAndOrganizationId(appEntityId, organizationId)
      ?: throw NotFoundException(Message.APP_NOT_FOUND)
  }

  @Transactional(readOnly = true)
  fun listOwned(organizationId: Long): List<App> {
    return appRepository.findAllByOrganizationIdOrderByNameAsc(organizationId)
  }

  /** How many organizations currently have the app installed. */
  @Transactional(readOnly = true)
  fun countInstalls(appEntityId: Long): Long {
    return appInstallRepository.countByRegisteredAppId(appEntityId)
  }

  /** Resolves an app by its app-level `client_id`. The caller must still verify the secret. */
  @Transactional(readOnly = true)
  fun resolveByClientId(clientId: String): App? {
    return appRepository.findByClientId(clientId)
  }

  /**
   * Returns the app registered under the manifest's id, registering it — owned by [organizationId] —
   * when nobody has yet. Runs in the caller's transaction so that registering and installing either
   * both happen or neither does.
   *
   * @param organizationId null registers an app owned by the server itself, matching a native install.
   */
  fun registerIfAbsent(
    organizationId: Long?,
    authorId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
  ): ResolveResult {
    val existing = appRepository.findByAppId(fetched.manifest.id)
    if (existing != null) return ResolveResult(existing, null)

    val clientId = APP_CLIENT_ID_PREFIX + keyGenerator.generate(128)
    val webhookSecret = keyGenerator.generate(256)
    val app =
      App().apply {
        this.organization = organizationId?.let { entityManager.getReference(Organization::class.java, it) }
        this.author = entityManager.getReference(UserAccount::class.java, authorId)
        this.appId = fetched.manifest.id
        this.manifestUrl = manifestUrl
        this.name = fetched.manifest.name
        this.baseUrl = fetched.manifest.baseUrl
        this.clientId = clientId
        this.webhookSecret = webhookSecret
      }
    val saved =
      try {
        appRepository.saveAndFlush(app)
      } catch (_: DataIntegrityViolationException) {
        throw BadRequestException(Message.APP_ALREADY_REGISTERED)
      }
    val issued = appSecretService.issueInitial(saved)

    return ResolveResult(
      app = saved,
      credentials =
        AppCredentials(
          clientId = clientId,
          clientSecret = issued.plaintextSecret,
          webhookSecret = webhookSecret,
        ),
    )
  }

  /**
   * The secret lifecycle deliveries to this app are signed with, minting one first if the app was
   * backfilled from an install that predates the app layer and therefore has none.
   */
  @Transactional
  fun resolveWebhookSecret(appId: Long): String {
    val app = appRepository.getReferenceById(appId)
    app.webhookSecret?.let { return it }
    val minted = keyGenerator.generate(256)
    app.webhookSecret = minted
    appRepository.save(app)
    return minted
  }

  /**
   * The app's app-level client id, minting one if the app was backfilled from an install that
   * predates the app layer. Issuing a secret is what first gives such an app a usable app-level
   * credential pair, so the id has to exist by then.
   */
  @Transactional
  fun resolveClientId(appEntityId: Long): String {
    val app = appRepository.getReferenceById(appEntityId)
    app.clientId?.let { return it }
    val minted = APP_CLIENT_ID_PREFIX + keyGenerator.generate(128)
    app.clientId = minted
    appRepository.save(app)
    return minted
  }

  fun summarize(app: App): AppSummary {
    return AppSummary(id = app.id, appId = app.appId, name = app.name)
  }

  companion object {
    const val APP_CLIENT_ID_PREFIX = "tgpub_"
    const val APP_CLIENT_SECRET_PREFIX = "tgpubs_"
    const val APP_CLIENT_SECRET_PREFIX_DISPLAY_LENGTH = 10
  }
}
