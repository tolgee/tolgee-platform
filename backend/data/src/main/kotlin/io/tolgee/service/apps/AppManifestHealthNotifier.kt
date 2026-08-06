package io.tolgee.service.apps

import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.email.EmailService
import io.tolgee.model.apps.App
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.util.runSentryCatching
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.util.Locale

/** Tells the owning organization that its app's manifest has stopped answering. */
@Component
class AppManifestHealthNotifier(
  private val organizationRoleRepository: OrganizationRoleRepository,
  private val emailService: EmailService,
  private val appsProperties: AppsProperties,
) {
  fun notifyUnhealthy(app: App) {
    val organization = app.organization ?: return

    organizationRoleRepository.getOwners(organization).forEach { owner ->
      runSentryCatching {
        emailService.sendEmailTemplate(
          recipient = owner.username,
          template = TEMPLATE,
          locale = Locale.ENGLISH,
          properties =
            mapOf(
              "recipientName" to owner.name,
              "appName" to HtmlUtils.htmlEscape(app.name),
              "manifestUrl" to HtmlUtils.htmlEscape(app.manifestUrl),
              "organizationName" to HtmlUtils.htmlEscape(organization.name),
              "lastError" to HtmlUtils.htmlEscape(app.manifestLastError ?: ""),
              "removeAfterDays" to appsProperties.manifestReapAfterUnhealthyDays.toString(),
            ),
        )
      }
    }
  }

  companion object {
    const val TEMPLATE = "app-manifest-unhealthy"
  }
}
