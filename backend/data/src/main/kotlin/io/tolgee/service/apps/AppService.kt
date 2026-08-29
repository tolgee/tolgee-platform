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
  fun findForAppAuth(appEntityId: Long): App? {
    return appRepository.findById(appEntityId).orElse(null)
  }

  @Transactional(readOnly = true)
  fun listOwned(organizationId: Long): List<App> {
    return appRepository.findAllByOrganizationIdOrderByNameAsc(organizationId)
  }

  @Transactional(readOnly = true)
  fun countInstalls(appEntityId: Long): Long {
    return appInstallRepository.countByRegisteredAppId(appEntityId)
  }

  @Transactional(readOnly = true)
  fun countInstallsByApp(appEntityIds: Collection<Long>): Map<Long, Long> {
    if (appEntityIds.isEmpty()) return emptyMap()
    return appInstallRepository
      .countInstallsByAppIds(appEntityIds)
      .associate { (it[0] as Long) to (it[1] as Long) }
  }

  @Transactional(readOnly = true)
  fun resolveByClientId(clientId: String): App? {
    return appRepository.findByClientId(clientId)
  }

  fun summarize(app: App): AppSummary {
    return AppSummary(id = app.id, appId = app.appId, name = app.name)
  }

  data class AppCredentials(
    val clientId: String,
    val clientSecret: String,
    val webhookSecret: String,
  )

  companion object {
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
