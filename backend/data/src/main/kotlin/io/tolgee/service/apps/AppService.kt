package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.exceptions.AppNotRegisteredException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
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
  private val currentDateProvider: CurrentDateProvider,
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

  /**
   * Apps offered to every organization that [organizationId] can still install — it neither owns nor
   * has already installed them.
   */
  @Transactional(readOnly = true)
  fun listAvailableToInstall(organizationId: Long): List<App> {
    return appRepository.findAvailableToInstall(organizationId)
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
   */
  fun registerIfAbsent(
    organizationId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
  ): ResolveResult {
    val existing = appRepository.findByAppId(fetched.manifest.id)
    if (existing != null) return ResolveResult(existing, null)

    val clientId = APP_CLIENT_ID_PREFIX + keyGenerator.generate(128)
    val webhookSecret = keyGenerator.generate(256)
    val app =
      App().apply {
        this.organization = entityManager.getReference(Organization::class.java, organizationId)
        this.appId = fetched.manifest.id
        this.manifestUrl = manifestUrl
        this.manifestScopes = joinScopes(fetched.scopes)
        this.name = fetched.manifest.name
        this.version = fetched.manifest.version
        this.baseUrl = fetched.manifest.baseUrl
        this.icon = fetched.icon
        this.manifestJson = fetched.rawJson
        this.clientId = clientId
        this.webhookSecret = webhookSecret
      }
    markManifestHealthy(app, currentDateProvider.date)
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

  fun summarize(app: App): AppSummary {
    return AppSummary(id = app.id, appId = app.appId, name = app.name)
  }

  companion object {
    /**
     * Every successful manifest read is a health check: it stamps the check time and clears any
     * failure state, whether it came from registration, a refresh, or the periodic sweep.
     */
    fun markManifestHealthy(
      app: App,
      checkedAt: java.util.Date,
    ) {
      app.manifestLastCheckedAt = checkedAt
      app.manifestFailureCount = 0
      app.manifestFirstFailedAt = null
      app.manifestLastError = null
      app.manifestLastFailureKind = null
      app.unhealthySince = null
      app.unhealthyNotifiedAt = null
    }

    /** The wire form of [io.tolgee.model.apps.App.manifestScopes]. */
    fun joinScopes(scopes: Set<io.tolgee.model.enums.Scope>): String =
      scopes.map { it.value }.sorted().joinToString(",")

    fun splitScopes(joined: String?): Set<String> =
      joined?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    const val APP_CLIENT_ID_PREFIX = "tgpub_"
    const val APP_CLIENT_SECRET_PREFIX = "tgpubs_"
    const val APP_CLIENT_SECRET_PREFIX_DISPLAY_LENGTH = 10
    const val APP_CLIENT_SECRET_SUFFIX_DISPLAY_LENGTH = 6
  }
}
