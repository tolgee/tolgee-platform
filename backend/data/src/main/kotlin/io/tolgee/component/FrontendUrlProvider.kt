package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.nullIfBlank
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class FrontendUrlProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  val requestDerivedUrl: String
    get() = stableUrl ?: getFromServerRequest()

  val stableUrl: String?
    get() = tolgeeProperties.frontEndUrl.nullIfBlank

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
    "${this.requestDerivedUrl}/organizations/$organizationSlug/subscriptions"

  fun getSelfHostedSubscriptionsUrl(organizationSlug: String): String =
    "${this.requestDerivedUrl}/organizations/$organizationSlug/subscriptions/self-hosted-ee"

  fun getInvoicesUrl(organizationSlug: String): String {
    return "${this.requestDerivedUrl}/organizations/$organizationSlug/invoices"
  }

  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String = "${this.requestDerivedUrl}/projects/$projectId/task?number=$taskId&detail=true"

  fun getMyTasksUrl(): String = "${this.requestDerivedUrl}/my-tasks"

  fun getAccountSecurityUrl(): String = "${this.requestDerivedUrl}/account/security"

  fun getNotificationSettingsUrl(): String = "${this.requestDerivedUrl}/account/notifications"

  fun getProjectUrl(projectId: Long): String = "${this.requestDerivedUrl}/projects/$projectId"

  fun getMembersUrl(projectId: Long): String = "${getProjectUrl(projectId)}/manage/permissions"

  fun getTasksUrl(projectId: Long): String = "${getProjectUrl(projectId)}/tasks"
}
