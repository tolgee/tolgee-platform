package io.tolgee.service.notification.digest

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.model.notifications.NotificationType
import io.tolgee.util.I18n
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

data class DigestRow(
  val notificationId: Long,
  val projectId: Long,
  val projectName: String,
  val type: NotificationType,
  val count: Int,
  val languageNames: List<String>,
  val branchNames: List<String>,
)

data class ComposedDigest(
  val subject: String,
  val html: String,
)

@Component
class NotificationDigestComposer(
  private val i18n: I18n,
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  fun compose(
    rows: List<DigestRow>,
    entityCap: Int,
  ): ComposedDigest {
    val projects =
      rows.groupBy { it.projectId }.values.joinToString("<br/><br/>") { projectRows ->
        val first = projectRows.first()
        val link = frontendUrlProvider.getProjectUrl(first.projectId)
        val lines =
          projectRows.sortedBy { it.type.ordinal }.joinToString("<br/>") { line(it, entityCap) }
        """<b><a href="$link">${HtmlUtils.htmlEscape(first.projectName)}</a></b><br/>$lines"""
      }
    val body = "${i18n.translate("notifications.email.digest.intro")}<br/><br/>$projects"
    return ComposedDigest(
      subject = i18n.translate("notifications.email.digest.subject"),
      html =
        i18n.translate("notifications.email.template", body, frontendUrlProvider.getNotificationSettingsUrl()),
    )
  }

  private fun line(
    row: DigestRow,
    entityCap: Int,
  ): String {
    val count =
      if (row.count >= entityCap) i18n.translate("notifications.email.digest.count-capped") else row.count.toString()
    val languages = HtmlUtils.htmlEscape(row.languageNames.joinToString(", "))
    val line = i18n.translate("notifications.email.digest.line.${row.type.name}", count, languages)
    if (row.branchNames.isEmpty()) return line
    val branches = HtmlUtils.htmlEscape(row.branchNames.joinToString(", "))
    return "$line ${i18n.translate("notifications.email.digest.branches", branches)}"
  }
}
