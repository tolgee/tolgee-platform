package io.tolgee.service.apps

import io.tolgee.exceptions.limits.PlanLimitExceededAppsException
import io.tolgee.repository.apps.AppRepository
import org.springframework.stereotype.Component

/**
 * Self-hosted semantics: the server holds at most N registered apps; installing an
 * already-registered app into more organizations is free.
 */
@Component
class ServerAppsLimitGuard(
  private val appsLimitProvider: AppsLimitProvider,
  private val appRepository: AppRepository,
) : AppsLimitGuard {
  override fun checkAppsLimit(
    organizationId: Long,
    registersNewApp: Boolean,
  ) {
    if (!registersNewApp) return
    val limit = appsLimitProvider.getAppsLimit(organizationId)
    if (limit < 0) return
    val registered = appRepository.count()
    if (registered + 1 > limit) {
      throw PlanLimitExceededAppsException(required = registered + 1, limit = limit)
    }
  }
}
