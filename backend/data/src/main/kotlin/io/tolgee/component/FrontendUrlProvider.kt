package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class FrontendUrlProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  val url: String
    get() {
      if (!tolgeeProperties.frontEndUrl.isNullOrBlank()) {
        return tolgeeProperties.frontEndUrl!!
      }

      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      builder.replacePath("")
      builder.replaceQuery("")
      return builder.build().toUriString()
    }

  fun getSubscriptionsUrl(organizationSlug: String): String =
    "${this.url}/organizations/$organizationSlug/subscriptions"

  fun getSelfHostedSubscriptionsUrl(organizationSlug: String): String =
    "${this.url}/organizations/$organizationSlug/subscriptions/self-hosted-ee"

  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String = "${this.url}/projects/$projectId/task?number=$taskId&detail=true"

  fun getMyTasksUrl(): String = "${this.url}/my-tasks"

  fun getAccountSecurityUrl(): String = "${this.url}/account/security"

  fun getNotificationSettingsUrl(): String = "${this.url}/account/notifications"
}
