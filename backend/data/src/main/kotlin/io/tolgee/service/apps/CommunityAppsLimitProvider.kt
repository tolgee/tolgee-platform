package io.tolgee.service.apps

import org.springframework.stereotype.Component

@Component
class CommunityAppsLimitProvider : AppsLimitProvider {
  override fun getAppsLimit(organizationId: Long): Long = COMMUNITY_APPS_LIMIT

  companion object {
    const val COMMUNITY_APPS_LIMIT = 3L
  }
}
