package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.AppNotRegisteredException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.apps.App
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.repository.apps.AppRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppService(
  private val appRepository: AppRepository,
  private val appInstallRepository: AppInstallRepository,
) {
  data class AppSummary(
    val id: Long,
    val appId: String,
    val name: String,
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
   * else's app must not reach it here - administering an app is the owner's alone.
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
  fun getRegistered(appEntityId: Long): App {
    return appRepository.findById(appEntityId).orElseThrow { NotFoundException(Message.APP_NOT_FOUND) }
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

  /** How many organizations hold each app, in one query, for a list of apps. */
  @Transactional(readOnly = true)
  fun countInstallsByApp(appEntityIds: Collection<Long>): Map<Long, Long> {
    if (appEntityIds.isEmpty()) return emptyMap()
    return appInstallRepository
      .countInstallsByAppIds(appEntityIds)
      .associate { (it[0] as Long) to (it[1] as Long) }
  }

  /** Resolves an app by its app-level `client_id`. The caller must still verify the secret. */
  @Transactional(readOnly = true)
  fun resolveByClientId(clientId: String): App? {
    return appRepository.findByClientId(clientId)
  }

  fun summarize(app: App): AppSummary {
    return AppSummary(id = app.id, appId = app.appId, name = app.name)
  }

  /** The app-level credentials, disclosed only in the response to registering the app. */
  data class AppCredentials(
    val clientId: String,
    val clientSecret: String,
    val webhookSecret: String,
  )

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
