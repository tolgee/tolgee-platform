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
      val frontEndUrlFromProperties = tolgeeProperties.frontEndUrl
      if (!frontEndUrlFromProperties.isNullOrBlank()) {
        return frontEndUrlFromProperties
      }

      return getFromServerRequest()
    }

  private fun getFromServerRequest(): String {
    try {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      builder.replacePath("")
      builder.replaceQuery("")
      return builder.build().toUriString()
    } catch (e: IllegalStateException) {
      if (e.message?.contains("No current ServletRequestAttributes") == true) {
        throw IllegalStateException(
          "Trying to find frontend url, but there is no current request. " +
            "You will have to specify frontend url in application properties.",
        )
      }
      throw e
    }
  }

  fun getSubscriptionsUrl(organizationSlug: String): String =
    "${this.url}/organizations/$organizationSlug/subscriptions"

  fun getSelfHostedSubscriptionsUrl(organizationSlug: String): String =
    "${this.url}/organizations/$organizationSlug/subscriptions/self-hosted-ee"

  fun getInvoicesUrl(organizationSlug: String): String {
    return "${this.url}/organizations/$organizationSlug/invoices"
  }

  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String = "${this.url}/projects/$projectId/task?number=$taskId&detail=true"

  fun getMyTasksUrl(): String = "${this.url}/my-tasks"

  fun getAccountSecurityUrl(): String = "${this.url}/account/security"

  fun getNotificationSettingsUrl(): String = "${this.url}/account/notifications"

  fun getProjectUrl(projectId: Long): String = "${this.url}/projects/$projectId"

  fun getMembersUrl(projectId: Long): String = "${getProjectUrl(projectId)}/manage/permissions"

  fun getTasksUrl(projectId: Long): String = "${getProjectUrl(projectId)}/tasks"
}
