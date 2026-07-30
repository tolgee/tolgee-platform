package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
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
    val attributes =
      RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        ?: throw IllegalStateException(
          "Trying to find frontend url, but there is no current request. " +
            "You will have to specify frontend url in application properties.",
        )
    val builder = ServletUriComponentsBuilder.fromRequestUri(attributes.request)
    builder.replacePath("")
    // fromRequestUri populates neither, but this url is embedded in outbound email and
    // fromRequest does populate the query — so the stripping stays regardless.
    builder.replaceQuery("")
    builder.fragment(null)
    return builder.build().toUriString()
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
